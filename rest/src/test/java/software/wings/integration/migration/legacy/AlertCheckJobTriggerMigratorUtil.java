package software.wings.integration.migration.legacy;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.scheduler.AlertCheckJob;
import software.wings.scheduler.QuartzScheduler;

/**
 * @author brett on 10/17/17
 */
@Integration
@Ignore
public class AlertCheckJobTriggerMigratorUtil extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  /**
   * Run this test by specifying VM argument -DsetupScheduler="true"
   */
  @Test
  public void scheduleCronForAlertCheck() {
    PageRequest<Account> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    System.out.println("Retrieving accounts");
    PageResponse<Account> pageResponse = wingsPersistence.query(Account.class, pageRequest);

    if (pageResponse.isEmpty() || isEmpty(pageResponse.getResponse())) {
      System.out.println("No accounts found");
      return;
    }
    pageResponse.getResponse().forEach(account -> {
      System.out.println("Creating alert check scheduler for account " + account.getUuid());
      // deleting the old
      AlertCheckJob.add(jobScheduler, account);
    });
  }
}
