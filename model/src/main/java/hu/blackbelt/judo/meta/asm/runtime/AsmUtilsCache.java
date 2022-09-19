package hu.blackbelt.judo.meta.asm.runtime;

/*-
 * #%L
 * Judo :: Asm :: Model
 * %%
 * Copyright (C) 2018 - 2022 BlackBelt Technology
 * %%
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is
 * available at https://www.gnu.org/software/classpath/license.html.
 * 
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 * #L%
 */

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EReference;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AsmUtilsCache {

    private final Map<String, Optional<EClassifier>> classifiersByFqName = new ConcurrentHashMap<>();

    private final Map<String, Optional<EReference>> referencesByFqName = new ConcurrentHashMap<>();

    private final Map<String, Optional<EAttribute>> attributesByFqName = new ConcurrentHashMap<>();

    private final Map<String, Optional<EOperation>> operationsByFqName = new ConcurrentHashMap<>();

    private Map<Class, Collection> elementsByType = new ConcurrentHashMap<>();

    public void clear() {
        classifiersByFqName.clear();
        referencesByFqName.clear();
        attributesByFqName.clear();
        operationsByFqName.clear();
        elementsByType.clear();
    }

    public Map<String, Optional<EClassifier>> getClassifiersByFqName() {
        return classifiersByFqName;
    }

    public Map<String, Optional<EReference>> getReferencesByFqName() {
        return referencesByFqName;
    }

    public Map<String, Optional<EAttribute>> getAttributesByFqName() {
        return attributesByFqName;
    }

    public Map<String, Optional<EOperation>> getOperationsByFqName() {
        return operationsByFqName;
    }

    public Map<Class, Collection> getElementsByType() {
        return elementsByType;
    }
}
