package software.wings.integration.service;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.beans.Application;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.setup.rest.AppResourceRestClient;
import software.wings.rules.Integration;

@Integration
@Ignore
public class WorkflowServiceIntegrationTest extends BaseIntegrationTest {
  @Inject private AppResourceRestClient appResourceRestClient;

  @Test
  public void executeBuildWorkflow() {
    Application seedApplication = appResourceRestClient.getSeedApplication(client);
  }
}
