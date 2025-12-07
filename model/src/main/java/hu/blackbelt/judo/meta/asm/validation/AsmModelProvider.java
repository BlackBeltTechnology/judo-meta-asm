package hu.blackbelt.judo.meta.asm.validation;

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

import hu.blackbelt.judo.meta.asm.runtime.AsmUtils;
import hu.blackbelt.judo.zeta.common.ModelProvider;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * ModelProvider implementation for ASM models.
 *
 * <p>Integrates ASM model traversal with the Judo Zeta validation framework.</p>
 */
public class AsmModelProvider implements ModelProvider {

    @Override
    public <T extends EObject> Collection<T> getAllContents(ResourceSet resourceSet, Class<T> type) {
        AsmUtils asmUtils = new AsmUtils(resourceSet);
        return asmUtils.all(type).collect(Collectors.toList());
    }
}
