/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.binding.api;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;

/**
 * Exception indicating that the {@link DataTreeProducer} has an open user transaction and cannot be
 * closed.
 */
public class DataTreeProducerBusyException extends DataTreeProducerException {

    private static final long serialVersionUID = 1L;


    public DataTreeProducerBusyException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(Preconditions.checkNotNull(message, "message"), cause);
    }

    public DataTreeProducerBusyException(@Nonnull final String message) {
        super(Preconditions.checkNotNull(message, "message"));
    }

}
