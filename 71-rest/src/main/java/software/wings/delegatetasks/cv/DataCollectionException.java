package software.wings.delegatetasks.cv;

public class DataCollectionException extends RuntimeException {
  public DataCollectionException(Exception e) {
    super(e);
  }

  public DataCollectionException(String message) {
    super(message);
  }
}
