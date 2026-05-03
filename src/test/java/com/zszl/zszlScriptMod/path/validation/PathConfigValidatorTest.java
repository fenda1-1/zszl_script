package com.zszl.zszlScriptMod.path.validation;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PathConfigValidatorTest {

    @Test
    public void extractLeafKeyStripsArrayIndexesBeforeMatchingExpressionKeys() throws Exception {
        Method method = PathConfigValidator.class.getDeclaredMethod("extractLeafKey", String.class);
        method.setAccessible(true);

        assertEquals("expressions", method.invoke(null, "expressions[0]"));
        assertEquals("expressions", method.invoke(null, "params.expressions[0]"));
        assertEquals("expression", method.invoke(null, "conditions[2].expression"));
    }

    @Test
    public void expressionArrayEntriesAreSkippedDuringVariableReferenceValidation() throws Exception {
        Method method = PathConfigValidator.class.getDeclaredMethod(
                "shouldSkipVariableReferenceValidation", String.class, String.class, String.class);
        method.setAccessible(true);

        boolean skipped = (Boolean) method.invoke(null, "expressions", "expressions[0]", "name");
        assertTrue(skipped);
    }
}
