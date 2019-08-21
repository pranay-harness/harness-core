package io.harness.notifications.conditions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

import com.google.common.collect.Lists;

import io.harness.category.element.UnitTests;
import io.harness.notifications.beans.CVAlertFilters;
import io.harness.notifications.beans.Conditions;
import io.harness.notifications.beans.Conditions.Operator;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertFilter;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;
import software.wings.verification.CVConfiguration;

import java.util.List;

public class CVFilterMatcherTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void testMatch() {
    List<String> appIds = Lists.newArrayList("app1", "app2");
    List<String> envIds = Lists.newArrayList("env1", "env2", "env3");
    List<String> cvConfigIds = Lists.newArrayList("cvConfig1", "cvConfig2", "cvConfig3", "cvConfig4");

    AlertFilter filter = new AlertFilter(AlertType.CONTINUOUS_VERIFICATION_ALERT,
        new Conditions(Operator.MATCHING, null,
            CVAlertFilters.builder()
                .appIds(appIds)
                .envIds(envIds)
                .cvConfigIds(cvConfigIds)
                .alertMinThreshold(0.3)
                .build()));
    CVConfiguration cvConfiguration = new CVConfiguration();
    cvConfiguration.setUuid("cvConfig1");
    cvConfiguration.setAppId("app1");
    cvConfiguration.setEnvId("env3");

    Alert cvAlert =
        Alert.builder()
            .type(AlertType.CONTINUOUS_VERIFICATION_ALERT)
            .alertData(
                ContinuousVerificationAlertData.builder().cvConfiguration(cvConfiguration).riskScore(0.4).build())
            .appId("app1")
            .build();
    CVFilterMatcher cvFilterMatcher = new CVFilterMatcher(filter, cvAlert);
    assertThat(cvFilterMatcher.matchesCondition()).isTrue();

    // should not alert for diff env
    cvConfiguration.setEnvId("env4");
    assertFalse(cvFilterMatcher.matchesCondition());

    // reset
    cvConfiguration.setEnvId("env2");
    assertThat(cvFilterMatcher.matchesCondition()).isTrue();

    // should not alert for diff app
    cvAlert.setAppId("app3");
    assertFalse(cvFilterMatcher.matchesCondition());

    // reset
    cvAlert.setAppId("app1");
    assertThat(cvFilterMatcher.matchesCondition()).isTrue();

    // should not alert for diff cvConfig
    cvConfiguration.setUuid("cvConfig5");
    assertFalse(cvFilterMatcher.matchesCondition());

    // reset
    cvConfiguration.setUuid("cvConfig3");
    assertThat(cvFilterMatcher.matchesCondition()).isTrue();

    // should not alert for less threshold
    cvAlert.setAlertData(
        ContinuousVerificationAlertData.builder().cvConfiguration(cvConfiguration).riskScore(0.2).build());
    assertFalse(cvFilterMatcher.matchesCondition());

    // put things back, should alert again
    cvAlert.setAlertData(
        ContinuousVerificationAlertData.builder().cvConfiguration(cvConfiguration).riskScore(0.4).build());
    assertThat(cvFilterMatcher.matchesCondition()).isTrue();
  }
}
