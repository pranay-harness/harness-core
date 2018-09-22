package software.wings.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.yaml.Change;

import java.util.Map;

/**
 * @author rktummala on 12/18/17
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class YamlProcessingException extends HarnessException {
  private Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap;

  public YamlProcessingException(String message, Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap) {
    super(message);
    this.failedYamlFileChangeMap = failedYamlFileChangeMap;
  }

  public YamlProcessingException(
      String message, Throwable cause, Map<String, ChangeWithErrorMsg> failedYamlFileChangeMap) {
    super(message, cause);
    this.failedYamlFileChangeMap = failedYamlFileChangeMap;
  }

  @Data
  @lombok.Builder
  public static class ChangeWithErrorMsg {
    private Change change;
    private String errorMsg;
  }
}
