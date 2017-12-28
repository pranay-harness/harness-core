package software.wings.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.yaml.Change;

/**
 * @author rktummala on 12/18/17
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class YamlProcessingException extends HarnessException {
  private Change change;

  public YamlProcessingException(String message, Change change) {
    super(message);
    this.change = change;
  }

  public YamlProcessingException(String message, Throwable cause, Change change) {
    super(message, cause);
    this.change = change;
  }
}
