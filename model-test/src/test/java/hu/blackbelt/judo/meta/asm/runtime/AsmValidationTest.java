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

import com.google.common.collect.ImmutableList;
import hu.blackbelt.judo.meta.asm.AbstractAsmValidationTest;
import hu.blackbelt.judo.meta.asm.ValidatorType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Validation tests for ASM models.
 *
 * <p>Tests are parameterized to run against both EVL and Java validators,
 * ensuring parity between the two validation implementations.</p>
 *
 * <p>Currently, the ASM validation (asm.evl) has no rules defined, so
 * tests verify that an empty model passes validation with no errors.</p>
 */
public class AsmValidationTest extends AbstractAsmValidationTest {

    /**
     * Test that an empty model passes validation with no errors.
     *
     * <p>This test runs with both EVL and Java validators to ensure
     * they produce identical results (no errors, no warnings).</p>
     */
    @ParameterizedTest(name = "testEmptyModelPassesValidation [{0}]")
    @EnumSource(ValidatorType.class)
    void testEmptyModelPassesValidation(ValidatorType type) throws Exception {
        this.validatorType = type;
        initModel();

        // Empty model should pass validation with no errors
        runValidation(ImmutableList.of(), ImmutableList.of());
    }
}
