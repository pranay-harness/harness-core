package software.wings.service.impl;

import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.NewRelicConfig;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicApplicationInstance;
import software.wings.service.impl.newrelic.NewRelicMetric;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 10/10/17.
 */
public class NewRelicTest extends WingsBaseTest {
  @Inject private NewRelicDelegateService newRelicDelegateService;
  private NewRelicConfig newRelicConfig;
  private String accountId;

  @Before
  public void setup() {
    accountId = UUID.randomUUID().toString();
    newRelicConfig = NewRelicConfig.builder()
                         .accountId(accountId)
                         .newRelicUrl("https://api.newrelic.com")
                         .apiKey("5ed76b50ebcfda54b77cd1daaabe635bd7f2e13dc6c5b11".toCharArray())
                         .build();
  }

  @Test
  public void getAllApplications() throws IOException {
    List<NewRelicApplication> allApplications = newRelicDelegateService.getAllApplications(newRelicConfig);
    assertFalse(allApplications.isEmpty());
  }

  @Test
  public void getApplicationInstances() throws IOException {
    NewRelicApplication demoApp = getDemoApp();
    List<NewRelicApplicationInstance> applicationInstances =
        newRelicDelegateService.getApplicationInstances(newRelicConfig, demoApp.getId());
    assertFalse(applicationInstances.isEmpty());
  }

  @Test
  public void getMetricsNameToCollect() throws IOException {
    NewRelicApplication demoApp = getDemoApp();
    Collection<NewRelicMetric> metricsNameToCollect =
        newRelicDelegateService.getMetricsNameToCollect(newRelicConfig, demoApp.getId());
    assertFalse(metricsNameToCollect.isEmpty());
  }

  private NewRelicApplication getDemoApp() throws IOException {
    List<NewRelicApplication> allApplications = newRelicDelegateService.getAllApplications(newRelicConfig);
    for (NewRelicApplication application : allApplications) {
      if (application.getName().equals("rsingh-demo-app")) {
        return application;
      }
    }

    throw new IllegalStateException("Could not find application rsingh-demo-app");
  }
}
