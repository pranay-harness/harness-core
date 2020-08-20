package software.wings.processingcontrollers;

import com.google.inject.Inject;

import io.harness.persistence.ProcessingController;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.AccountService;

@Slf4j
public class DelegateProcessingController implements ProcessingController {
  @Inject private AccountService accountService;

  private static final long ALLOWED_NUMBER_OF_DAYS_SINCE_EXPIRY = Integer.MAX_VALUE;

  @Override
  public boolean canProcessAccount(String accountId) {
    return accountService.canProcessAccount(accountId, ALLOWED_NUMBER_OF_DAYS_SINCE_EXPIRY);
  }
}
