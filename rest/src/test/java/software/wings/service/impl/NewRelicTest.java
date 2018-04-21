package software.wings.service.impl;

import static org.junit.Assert.assertFalse;

import com.google.inject.Inject;

import io.harness.rule.RepeatRule.Repeat;
import io.harness.time.Timestamp;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.NewRelicConfig;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicApplicationInstance;
import software.wings.service.impl.newrelic.NewRelicDelgateServiceImpl;
import software.wings.service.impl.newrelic.NewRelicMetric;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
  @Repeat(times = 5, successes = 1)
  @Ignore
  public void getAllApplications() throws IOException {
    List<NewRelicApplication> allApplications =
        newRelicDelegateService.getAllApplications(newRelicConfig, Collections.emptyList());
    assertFalse(allApplications.isEmpty());
  }

  @Test
  @Repeat(times = 5, successes = 1)
  @Ignore
  public void getApplicationInstances() throws IOException {
    NewRelicApplication demoApp = getDemoApp();
    List<NewRelicApplicationInstance> applicationInstances =
        newRelicDelegateService.getApplicationInstances(newRelicConfig, Collections.emptyList(), demoApp.getId());
    assertFalse(applicationInstances.isEmpty());
  }

  @Test
  @Repeat(times = 5, successes = 1)
  @Ignore
  public void getMetricsNameToCollect() throws IOException {
    NewRelicApplication demoApp = getDemoApp();
    Collection<NewRelicMetric> metricsNameToCollect =
        newRelicDelegateService.getMetricsNameToCollect(newRelicConfig, Collections.emptyList(), demoApp.getId());
    assertFalse(metricsNameToCollect.isEmpty());
  }

  private NewRelicApplication getDemoApp() throws IOException {
    List<NewRelicApplication> allApplications =
        newRelicDelegateService.getAllApplications(newRelicConfig, Collections.emptyList());
    for (NewRelicApplication application : allApplications) {
      if (application.getName().equals("rsingh-demo-app")) {
        return application;
      }
    }

    throw new IllegalStateException("Could not find application rsingh-demo-app");
  }

  @Test
  public void testTimeStampCreations() {
    NewRelicDelgateServiceImpl.dateFormatter.format(new Date(Timestamp.minuteBoundary(1513463100000L)))
        .equals("2017-12-16T14:25:00-0800");
  }
}
