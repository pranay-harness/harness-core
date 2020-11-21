package software.wings.service.impl.event;

import io.harness.event.model.EventInfo;

import software.wings.beans.Account;

import lombok.Value;

@Value
public class AccountEntityEvent implements EventInfo {
  private Account account;
}
