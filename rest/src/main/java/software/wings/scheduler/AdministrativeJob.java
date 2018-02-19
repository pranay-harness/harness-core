package software.wings.scheduler;

import com.google.inject.Inject;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.security.SecretManager;

import java.util.concurrent.ExecutorService;

/**
 * Cron that runs every mid night to cleanup the data
 * Created by sgurubelli on 7/19/17.
 */
public class AdministrativeJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(AdministrativeJob.class);

  @Inject private SecretManager secretManager;
  @Inject private ExecutorService executorService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.info("Running Administrative Job asynchronously and returning");
    executorService.submit(this ::executeInternal);
  }

  private void executeInternal() {
    logger.info("Running Administrative Job");
    secretManager.checkAndAlertForInvalidManagers();
    logger.info("Administrative Job complete");
  }
}
