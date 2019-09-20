package software.wings.beans;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class AccountEvent {
  private AccountEventType accountEventType;
  private String customMsg;
  private String category;
  private Map<String, String> properties;
}
