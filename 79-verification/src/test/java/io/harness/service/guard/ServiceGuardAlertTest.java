package io.harness.service.guard;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL;
import static software.wings.sm.StateType.SUMO;

import io.harness.VerificationBaseIntegrationTest;
import io.harness.category.element.IntegrationTests;
import io.harness.rest.RestResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.alerts.AlertCategory;
import software.wings.alerts.AlertStatus;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.Alert.AlertKeys;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.verification.CVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;

import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
@Slf4j
public class ServiceGuardAlertTest extends VerificationBaseIntegrationTest {
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  @Test
  @Category(IntegrationTests.class)
  public <T extends CVConfiguration> void testLogsConfigurationResetBaseline() {
    final String appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUuid()).build());
    LogsCVConfiguration logsCVConfiguration = new LogsCVConfiguration();
    logsCVConfiguration.setName("Config 1");
    logsCVConfiguration.setAppId(appId);
    logsCVConfiguration.setEnvId(generateUuid());
    logsCVConfiguration.setServiceId(generateUuid());
    logsCVConfiguration.setConnectorId(generateUuid());
    logsCVConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);
    logsCVConfiguration.setAlertEnabled(true);
    logsCVConfiguration.setAlertThreshold(0.5);
    logsCVConfiguration.setBaselineStartMinute(100);
    logsCVConfiguration.setBaselineEndMinute(200);
    logsCVConfiguration.setQuery("query1");

    String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + SUMO;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(logsCVConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = VERIFICATION_API_BASE + "/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_SAVE_24X7_ANALYSIS_RECORDS_URL + "?cvConfigId=" + savedObjectUuid
        + "&appId=" + appId + "&analysisMinute=100"
        + "&taskId=" + generateUuid();
    target = client.target(url);
    getRequestBuilderWithLearningAuthHeader(target).post(
        entity(LogMLAnalysisRecord.builder().score(0.4).build(), APPLICATION_JSON),
        new GenericType<RestResponse<String>>() {});

    List<Alert> alerts = wingsPersistence.createQuery(Alert.class).filter(AlertKeys.appId, appId).asList();
    assertThat(alerts.isEmpty()).isTrue();

    url = VERIFICATION_API_BASE + "/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_SAVE_24X7_ANALYSIS_RECORDS_URL + "?cvConfigId=" + savedObjectUuid
        + "&appId=" + appId + "&analysisMinute=105"
        + "&taskId=" + generateUuid();
    target = client.target(url);
    getRequestBuilderWithLearningAuthHeader(target).post(
        entity(LogMLAnalysisRecord.builder().score(0.6).build(), APPLICATION_JSON),
        new GenericType<RestResponse<String>>() {});

    sleep(ofMillis(5000));
    alerts = wingsPersistence.createQuery(Alert.class).filter(AlertKeys.appId, appId).asList();
    assertThat(alerts).hasSize(1);
    Alert alert = alerts.get(0);
    assertThat(alert.getAccountId()).isEqualTo(accountId);
    assertThat(alert.getType()).isEqualTo(AlertType.CONTINUOUS_VERIFICATION_ALERT);
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.Open);
    assertThat(alert.getCategory()).isEqualTo(AlertCategory.ContinuousVerification);
    ContinuousVerificationAlertData alertData = (ContinuousVerificationAlertData) alert.getAlertData();
    assertEquals(savedObjectUuid, alertData.getCvConfiguration().getUuid());
    assertEquals(0.6, alertData.getRiskScore(), 0.0);

    // send another alert
    url = VERIFICATION_API_BASE + "/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_SAVE_24X7_ANALYSIS_RECORDS_URL + "?cvConfigId=" + savedObjectUuid
        + "&appId=" + appId + "&analysisMinute=120"
        + "&taskId=" + generateUuid();
    target = client.target(url);
    getRequestBuilderWithLearningAuthHeader(target).post(
        entity(LogMLAnalysisRecord.builder().score(0.6).build(), APPLICATION_JSON),
        new GenericType<RestResponse<String>>() {});

    sleep(ofMillis(5000));
    alerts = wingsPersistence.createQuery(Alert.class).filter(AlertKeys.appId, appId).asList();
    assertThat(alerts).hasSize(2);

    // snooze should not create new alert
    wingsPersistence.updateField(
        CVConfiguration.class, savedObjectUuid, "snoozeStartTime", System.currentTimeMillis() - CRON_POLL_INTERVAL);
    wingsPersistence.updateField(
        CVConfiguration.class, savedObjectUuid, "snoozeEndTime", System.currentTimeMillis() + CRON_POLL_INTERVAL);
    url = VERIFICATION_API_BASE + "/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_SAVE_24X7_ANALYSIS_RECORDS_URL + "?cvConfigId=" + savedObjectUuid
        + "&appId=" + appId + "&analysisMinute=135"
        + "&taskId=" + generateUuid();
    target = client.target(url);
    getRequestBuilderWithLearningAuthHeader(target).post(
        entity(LogMLAnalysisRecord.builder().score(0.6).build(), APPLICATION_JSON),
        new GenericType<RestResponse<String>>() {});

    sleep(ofMillis(5000));
    alerts = wingsPersistence.createQuery(Alert.class).filter(AlertKeys.appId, appId).asList();
    assertThat(alerts).hasSize(2);
  }
}
