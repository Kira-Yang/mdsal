/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.mdsal.binding.yang.types;

import java.util.List;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.type.IntegerTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.RangeConstraint;

/**
 * Mock Integer Type Definition designated to increase branch coverage in test cases.
 *
 * @author Lukas Sedlak &lt;lsedlak@cisco.com&gt;
 */
final class TestIntegerTypeDefinition implements IntegerTypeDefinition {

    @Override public List<RangeConstraint> getRangeConstraints() {
        return null;
    }

    @Override public IntegerTypeDefinition getBaseType() {
        return null;
    }

    @Override public String getUnits() {
        return null;
    }

    @Override public Object getDefaultValue() {
        return null;
    }

    @Override public QName getQName() {
        return null;
    }

    @Override public SchemaPath getPath() {
        return null;
    }

    @Override public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return null;
    }

    @Override public String getDescription() {
        return null;
    }

    @Override public String getReference() {
        return null;
    }

    @Override public Status getStatus() {
        return null;
    }
}
