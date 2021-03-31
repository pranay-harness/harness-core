package software.wings.delegatetasks.azure.arm.deployment.validator;

import static io.harness.azure.model.AzureConstants.BLUEPRINT_JSON_FILE_BLANK_VALIDATION_MSG;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;

import software.wings.delegatetasks.azure.common.validator.Validator;

public class BlueprintJsonValidator implements Validator<String> {
  @Override
  public void validate(String blueprintJson) {
    isValidJson(blueprintJson);
  }

  private void isValidJson(final String blueprintJson) {
    if (isBlank(blueprintJson)) {
      throw new InvalidArgumentsException(BLUEPRINT_JSON_FILE_BLANK_VALIDATION_MSG);
    }

    try {
      JsonUtils.readTree(blueprintJson);
    } catch (Exception e) {
      throw new InvalidArgumentsException("Invalid Blueprint JSON file", e, WingsException.USER);
    }
  }
}
