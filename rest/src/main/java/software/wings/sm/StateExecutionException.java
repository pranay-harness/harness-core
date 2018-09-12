package software.wings.sm;

import io.harness.exception.WingsException;

/**
 * Created by peeyushaggarwal on 6/17/16.
 */
public class StateExecutionException extends WingsException {
  private static final long serialVersionUID = 6211853765310518441L;

  /**
   * Instantiates a new State execution exception.
   *
   * @param message the message
   */
  public StateExecutionException(String message) {
    super(message);
  }
}
