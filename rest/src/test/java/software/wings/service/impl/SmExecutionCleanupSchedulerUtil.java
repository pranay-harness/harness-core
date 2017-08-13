package software.wings.service.impl;

import static software.wings.dl.PageRequest.Builder.aPageRequest;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.dl.PageResponse;
import software.wings.rules.Integration;
import software.wings.service.intfc.AppService;
import software.wings.utils.Misc;

import javax.inject.Inject;

/**
 * Created by rishi on 4/9/17.
 */
@Integration
@Ignore
public class SmExecutionCleanupSchedulerUtil extends WingsBaseTest {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject AppService appService;

  @Test
  @Ignore
  public void recreateSmExecutionCleanupJobs() {
    AppServiceImpl appServiceImpl = (AppServiceImpl) appService;
    PageResponse<Application> applications = appService.list(aPageRequest().build(), false, 0, 0);
    applications.forEach(application -> {
      try {
        appServiceImpl.deleteCronForStateMachineExecutionCleanup(application.getUuid());
      } catch (Exception e) {
        logger.error(String.format("Error in delete schedule - appId: %s, name: %s", application.getUuid(),
                         application.getName()),
            e);
      }
      appServiceImpl.addCronForStateMachineExecutionCleanup(application);

    });
  }
}
