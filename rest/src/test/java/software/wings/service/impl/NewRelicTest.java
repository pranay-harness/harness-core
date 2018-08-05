package software.wings.service.impl;

import static org.junit.Assert.assertFalse;

import com.google.inject.Inject;

import io.harness.rule.RepeatRule.Repeat;
import io.harness.time.Timestamp;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import software.wings.WingsBaseTest;
import software.wings.beans.FeatureName;
import software.wings.beans.NewRelicConfig;
import software.wings.generator.SecretGenerator;
import software.wings.generator.SecretGenerator.SecretName;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicApplicationInstance;
import software.wings.service.impl.newrelic.NewRelicMetric;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;

import java.io.IOException;
import java.text.SimpleDateFormat;
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
  @Inject SecretGenerator secretGenerator;
  private NewRelicConfig newRelicConfig;
  private String accountId;
  static final String NEW_RELIC_DATE_FORMAT = "YYYY-MM-dd'T'HH:mm:ssZ";
  @Before
  public void setup() {
    accountId = UUID.randomUUID().toString();
    newRelicConfig = NewRelicConfig.builder()
                         .accountId(accountId)
                         .newRelicUrl("https://api.newrelic.com")
                         .apiKey(secretGenerator.decryptToCharArray(new SecretName("new_relic_api_key")))
                         .build();
  }

  @Test
  public void cvdemo() throws IOException {
    // DO NOT REMOVE CV_DEMO FEATURE FLAG
    FeatureName.valueOf("CV_DEMO");
  }

  @Test
  @Repeat(times = 5, successes = 1)
  @Ignore
  public void getAllApplications() throws IOException, CloneNotSupportedException {
    List<NewRelicApplication> allApplications =
        newRelicDelegateService.getAllApplications(newRelicConfig, Collections.emptyList(), null);
    assertFalse(allApplications.isEmpty());
  }

  @Test
  @Repeat(times = 5, successes = 1)
  @Ignore
  public void getApplicationInstances() throws IOException, CloneNotSupportedException {
    NewRelicApplication demoApp = getDemoApp();
    List<NewRelicApplicationInstance> applicationInstances =
        newRelicDelegateService.getApplicationInstances(newRelicConfig, Collections.emptyList(), demoApp.getId(), null);
    assertFalse(applicationInstances.isEmpty());
  }

  @Test
  @Repeat(times = 5, successes = 1)
  @Ignore
  public void getMetricsNameToCollect() throws IOException, CloneNotSupportedException {
    NewRelicApplication demoApp = getDemoApp();
    Collection<NewRelicMetric> metricsNameToCollect =
        newRelicDelegateService.getTxnNameToCollect(newRelicConfig, Collections.emptyList(), demoApp.getId(), null);
    assertFalse(metricsNameToCollect.isEmpty());
  }

  private NewRelicApplication getDemoApp() throws IOException, CloneNotSupportedException {
    List<NewRelicApplication> allApplications =
        newRelicDelegateService.getAllApplications(newRelicConfig, Collections.emptyList(), null);
    for (NewRelicApplication application : allApplications) {
      if (application.getName().equals("rsingh-demo-app")) {
        return application;
      }
    }

    throw new IllegalStateException("Could not find application rsingh-demo-app");
  }

  @Test
  public void testTimeStampCreations() {
    final SimpleDateFormat dateFormatter = new SimpleDateFormat(NEW_RELIC_DATE_FORMAT);
    dateFormatter.format(new Date(Timestamp.minuteBoundary(1513463100000L))).equals("2017-12-16T14:25:00-0800");
  }
}
