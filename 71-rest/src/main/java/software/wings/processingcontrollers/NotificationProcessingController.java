package software.wings.processingcontrollers;

import com.google.inject.Inject;

import io.harness.persistence.ProcessingController;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AccountStatus;
import software.wings.exception.AccountNotFoundException;
import software.wings.service.intfc.AccountService;

@Slf4j
public class NotificationProcessingController implements ProcessingController {
  @Inject private AccountService accountService;

  @Override
  public boolean canProcessAccount(String accountId) {
    String accountStatus;

    try {
      accountStatus = accountService.getAccountStatus(accountId);
    } catch (AccountNotFoundException ex) {
      log.warn("Skipping processing account {}. It does not exist", accountId, ex);
      return false;
    }
    return AccountStatus.ACTIVE.equals(accountStatus);
  }
}
