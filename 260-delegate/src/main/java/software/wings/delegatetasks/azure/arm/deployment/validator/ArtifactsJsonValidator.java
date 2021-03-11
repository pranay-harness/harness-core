package software.wings.delegatetasks.azure.arm.deployment.validator;

import static java.lang.String.format;

import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;

import java.util.Map;

public class ArtifactsJsonValidator implements Validator<Map<String, String>> {
  @Override
  public void validate(Map<String, String> artifactJsons) {
    artifactJsons.forEach(this::isValidJson);
  }

  private void isValidJson(String artifactName, String artifactJson) {
    try {
      JsonUtils.readTree(artifactJson);
    } catch (Exception e) {
      throw new InvalidArgumentsException(
          format("Invalid Artifact JSON, artifact file name: %s", artifactName), e, WingsException.USER);
    }
  }
}
