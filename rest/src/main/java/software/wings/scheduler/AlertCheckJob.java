package software.wings.scheduler;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.alert.AlertType;
import software.wings.beans.Delegate;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AlertService;

import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * @author brett on 10/17/17
 */
public class AlertCheckJob implements Job {
  private static final long MAX_HB_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

  private static final Logger logger = LoggerFactory.getLogger(AlertCheckJob.class);

  @Inject private AlertService alertService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    String accountId = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("accountId");
    logger.info("Checking account " + accountId + " for alert conditions.");

    List<Delegate> delegates =
        wingsPersistence.createQuery(Delegate.class).field("accountId").equal(accountId).asList();

    if (isEmpty(delegates)
        || delegates.stream().allMatch(
               delegate -> System.currentTimeMillis() - delegate.getLastHeartBeat() > MAX_HB_TIMEOUT)) {
      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.NoActiveDelegates,
          NoActiveDelegatesAlert.builder().accountId(accountId).build());
    }
  }
}
