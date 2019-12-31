package software.wings.scheduler;

import com.google.inject.Inject;

import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import software.wings.beans.Account;
import software.wings.service.intfc.security.VaultService;

@Slf4j
public class AdministrativeJob implements Job, Handler<Account> {
  public static final String ADMINISTRATIVE_CRON_NAME = "ADMINISTRATIVE_CRON_NAME";
  public static final String ADMINISTRATIVE_CRON_GROUP = "ADMINISTRATIVE_CRON_GROUP";

  @Inject private VaultService vaultService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    // this method will be deleted once the iterator goes in
  }

  @Override
  public void handle(Account account) {
    logger.info("renewing tokens for {}", account.getUuid());
    try {
      vaultService.renewTokens(account.getUuid());
      vaultService.appRoleLogin(account.getUuid());
    } catch (Exception e) {
      logger.info("Failed to renew vault token for account id {}", account.getUuid(), e);
    }
  }
}
