package io.harness.cdng;

import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.exception.InvalidArgumentsException;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.yaml.ParameterField;

import static io.harness.common.ParameterFieldHelper.getBooleanParameterFieldValue;

public class ParameterFieldBooleanValueHelper {
    public static boolean getParameterFieldBooleanValue(
            ParameterField<?> fieldValue, String fieldName, StepElementParameters stepElement) {
        return getParameterFieldBooleanValue(fieldValue, fieldName,
                String.format("%s step with identifier: %s", stepElement.getType(), stepElement.getIdentifier()));
    }

    public static boolean getParameterFieldBooleanValue(
            ParameterField<?> fieldValue, String fieldName, ManifestOutcome manifestOutcome) {
        return getParameterFieldBooleanValue(fieldValue, fieldName,
                String.format("%s manifest with identifier: %s", manifestOutcome.getType(), manifestOutcome.getIdentifier()));
    }

    public static boolean getParameterFieldBooleanValue(
            ParameterField<?> fieldValue, String fieldName, String description) {
        try {
            return getBooleanParameterFieldValue(fieldValue);
        } catch (Exception e) {
            String message = String.format("%s for field %s in %s", e.getMessage(), fieldName, description);
            throw new InvalidArgumentsException(message);
        }
    }
}
