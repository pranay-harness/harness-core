package software.wings.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccountEvent {
  private AccountEventType accountEventType;
  private String customMsg;
  private String category;
}
