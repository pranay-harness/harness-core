package software.wings.security.authentication.recaptcha;

import lombok.Value;
import org.slf4j.helpers.MessageFormatter;

@Value
public class MaxLoginAttemptExceededException extends Exception {
  private int limit;
  private int attempts;

  @Override
  public String getMessage() {
    return MessageFormatter.format("Login Attempts. limit={} attempts={}", limit, attempts).getMessage();
  }
}
