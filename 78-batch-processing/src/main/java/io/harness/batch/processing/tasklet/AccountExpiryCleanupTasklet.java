package io.harness.batch.processing.tasklet;

import com.google.inject.Singleton;

import io.harness.batch.processing.service.intfc.AccountExpiryService;
import io.harness.ccm.license.CeLicenseInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import software.wings.beans.Account;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.time.Instant;
import java.util.List;

@Slf4j
@Singleton
public class AccountExpiryCleanupTasklet implements Tasklet {
  @Autowired private AccountExpiryService accountExpiryService;
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
    List<Account> accounts = cloudToHarnessMappingService.getCeAccountsWithLicense();
    logger.info("Accounts batch size is AccountExpiryCleanupTasklet {} ", accounts.size());
    accounts.forEach(account -> {
      CeLicenseInfo ceLicenseInfo = account.getCeLicenseInfo();
      long expiryTime = ceLicenseInfo.getExpiryTimeWithGracePeriod();
      if (expiryTime != 0L && Instant.now().toEpochMilli() > expiryTime) {
        logger.info("Triggering Data Pipeline Clean up for account: {} ", account);
        accountExpiryService.dataPipelineCleanup(account);
      }
    });
    return null;
  }
}
