package software.wings.exception;

/**
 * Created by peeyushaggarwal on 10/17/16.
 */
public class WingsApiException extends RuntimeException {
  private static final long serialVersionUID = 2722172818755450949L;

  public WingsApiException() {}

  public WingsApiException(String message) {
    super(message);
  }

  public WingsApiException(String message, Throwable cause) {
    super(message, cause);
  }

  public WingsApiException(Throwable cause) {
    super(cause);
  }

  public WingsApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
