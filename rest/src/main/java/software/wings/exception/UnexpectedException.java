package software.wings.exception;

public class UnexpectedException extends WingsException {
  public UnexpectedException() {
    super("This should not be happening");
  }
}
