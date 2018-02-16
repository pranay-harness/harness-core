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
  private Map<Change, String> failedChangeErrorMsgMap;

  public YamlProcessingException(String message, Map<Change, String> failedChangeErrorMsgMap) {
    super(message);
    this.failedChangeErrorMsgMap = failedChangeErrorMsgMap;
  }

  public YamlProcessingException(String message, Throwable cause, Map<Change, String> failedChangeErrorMsgMap) {
    super(message, cause);
    this.failedChangeErrorMsgMap = failedChangeErrorMsgMap;
  }
}
