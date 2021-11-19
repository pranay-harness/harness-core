package software.wings.exception;

public class InvalidYamlNameException extends RuntimeException {
  public InvalidYamlNameException(String message, Throwable cause) {
    super(message, cause);
  }
  public InvalidYamlNameException(String message) {
    super(message);
  }
}
