package software.wings.service.impl.event;

import io.harness.event.model.EventInfo;
import lombok.Value;
import software.wings.beans.Account;

@Value
public class AccountEntityEvent implements EventInfo {
  private Account account;
}
