package software.wings.delegatetasks.cv;

@Deprecated
public class RateLimitExceededException extends DataCollectionException {
  public RateLimitExceededException(Exception e) {
    super(e);
  }

  public RateLimitExceededException(String message) {
    super(message);
  }
}
