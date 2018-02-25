package software.wings.service.impl;

import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.dl.PageResponse;
import software.wings.rules.Integration;
import software.wings.scheduler.QuartzScheduler;
import software.wings.scheduler.StateMachineExecutionCleanupJob;
import software.wings.service.intfc.AppService;

/**
 * Created by rishi on 4/9/17.
 */
@Integration
@Ignore
public class SmExecutionCleanupSchedulerUtil extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(SmExecutionCleanupSchedulerUtil.class);
  @Inject AppService appService;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  @Test
  @Ignore
  public void recreateSmExecutionCleanupJobs() {
    AppServiceImpl appServiceImpl = (AppServiceImpl) appService;
    PageResponse<Application> applications = appService.list(aPageRequest().build(), false, 0, 0);
    applications.forEach(application -> { StateMachineExecutionCleanupJob.add(jobScheduler, application.getUuid()); });
  }
}
