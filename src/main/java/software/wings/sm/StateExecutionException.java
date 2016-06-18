package software.wings.sm;

import software.wings.exception.WingsException;

/**
 * Created by peeyushaggarwal on 6/17/16.
 */
public class StateExecutionException extends WingsException {
  private static final long serialVersionUID = 6211853765310518441L;

  public StateExecutionException(String message) {
    super(message);
  }
}
