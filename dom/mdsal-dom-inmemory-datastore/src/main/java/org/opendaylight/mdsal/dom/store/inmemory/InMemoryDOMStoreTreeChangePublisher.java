/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.dom.store.inmemory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.spi.AbstractDOMDataTreeChangeListenerRegistration;
import org.opendaylight.mdsal.dom.spi.store.AbstractDOMStoreTreeChangePublisher;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager.BatchedInvoker;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InMemoryDOMStoreTreeChangePublisher extends AbstractDOMStoreTreeChangePublisher {
    private static final BatchedInvoker<AbstractDOMDataTreeChangeListenerRegistration<?>, DataTreeCandidate>
        MANAGER_INVOKER = (listener, notifications) -> {
            final DOMDataTreeChangeListener inst = listener.getInstance();
            if (inst != null) {
                inst.onDataTreeChanged(ImmutableList.copyOf(notifications));
            }
        };
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDOMStoreTreeChangePublisher.class);

    private final QueuedNotificationManager<AbstractDOMDataTreeChangeListenerRegistration<?>, DataTreeCandidate>
        notificationManager;

    InMemoryDOMStoreTreeChangePublisher(final ExecutorService listenerExecutor, final int maxQueueSize) {
        notificationManager = QueuedNotificationManager.create(listenerExecutor, MANAGER_INVOKER, maxQueueSize,
                "DataTreeChangeListenerQueueMgr");
    }

    private InMemoryDOMStoreTreeChangePublisher(QueuedNotificationManager<
            AbstractDOMDataTreeChangeListenerRegistration<?>, DataTreeCandidate> notificationManager) {
        this.notificationManager = notificationManager;
    }

    @Override
    protected void notifyListener(AbstractDOMDataTreeChangeListenerRegistration<?> registration,
            Collection<DataTreeCandidate> changes) {
        LOG.debug("Enqueueing candidates {} for registration {}", changes, registration);
        notificationManager.submitNotifications(registration, changes);
    }

    @Override
    protected synchronized void registrationRemoved(
            final AbstractDOMDataTreeChangeListenerRegistration<?> registration) {
        LOG.debug("Closing registration {}", registration);

        // FIXME: remove the queue for this registration and make sure we clear it
    }

    <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(
            final YangInstanceIdentifier treeId, final L listener, final DataTreeSnapshot snapshot) {
        final AbstractDOMDataTreeChangeListenerRegistration<L> reg = registerTreeChangeListener(treeId, listener);

        final Optional<NormalizedNode<?, ?>> node = snapshot.readNode(YangInstanceIdentifier.EMPTY);
        if (node.isPresent()) {
            final DataTreeCandidate candidate = DataTreeCandidates.fromNormalizedNode(
                    YangInstanceIdentifier.EMPTY, node.get());

            InMemoryDOMStoreTreeChangePublisher publisher =
                    new InMemoryDOMStoreTreeChangePublisher(notificationManager);
            publisher.registerTreeChangeListener(treeId, listener);
            publisher.publishChange(candidate);
        }

        return reg;
    }

    synchronized void publishChange(@Nonnull final DataTreeCandidate candidate) {
        // Runs synchronized with registrationRemoved()
        processCandidateTree(candidate);
    }
}
