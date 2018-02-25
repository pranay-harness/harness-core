package software.wings.integration.migration;

import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.service.intfc.TriggerService;

/**
 * Created by sgurubelli on 01/22/18.
 */
@Integration
@Ignore
public class TriggerMigrationUtil extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Inject TriggerService triggerService;

  /***
   * Updates triggers by AppId to sync
   */
  @Test
  public void updateTriggersByApp() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    System.out.println("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    pageResponse.getResponse().forEach(application -> {
      System.out.println("Updating triggers for app {} = " + application.getName());
      triggerService.updateByApp(application.getAppId());
      System.out.println("Updated triggers for app {} = " + application.getName());
    });
  }
}
