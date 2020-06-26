package software.wings.processingcontrollers;

import com.google.inject.Inject;

import io.harness.persistence.ProcessingController;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.service.intfc.AccountService;

@Slf4j
public class NotificationProcessingController implements ProcessingController {
  @Inject private AccountService accountService;

  private static final long ALLOWED_NUMBER_OF_DAYS_SINCE_EXPIRY = 4;

  @Override
  public boolean shouldProcessAccount(String accountId) {
    Account account = accountService.getFromCacheWithFallback(accountId);
    String accountStatus = account.getLicenseInfo().getAccountStatus();
    if (AccountStatus.EXPIRED.equals(accountStatus)) {
      long numberOfDaysSinceExpiry = account.getNumberOfDaysSinceExpiry(System.currentTimeMillis());
      return ALLOWED_NUMBER_OF_DAYS_SINCE_EXPIRY > numberOfDaysSinceExpiry;
    }
    return AccountStatus.ACTIVE.equals(accountStatus);
  }
}
