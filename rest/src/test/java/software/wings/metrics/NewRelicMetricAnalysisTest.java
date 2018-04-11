package software.wings.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.scheduler.NewRelicMetricNameCollectionJob.NEW_RELIC_METRIC_COLLECTION_ALERT_THRESHOLD;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.alerts.AlertCategory;
import software.wings.alerts.AlertSeverity;
import software.wings.alerts.AlertStatus;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.NewRelicMetricNameCollectionAlert;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.NewRelicMetricNameCollectionJob;
import software.wings.service.impl.newrelic.NewRelicMetric;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricNames;
import software.wings.service.impl.newrelic.NewRelicMetricNames.WorkflowInfo;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 9/7/17.
 */
public class NewRelicMetricAnalysisTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  @Mock private MetricDataAnalysisService metricDataAnalysisService;
  @Mock private DelegateService delegateService;
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Inject private NewRelicMetricNameCollectionJob metricNameCollectionJob;
  @Inject private WingsPersistence wingsPersistence;

  @Before
  public void setup() throws IOException {
    initMocks(this);
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    when(delegateService.queueTask(anyObject())).thenReturn(UUID.randomUUID().toString());
    when(waitNotifyEngine.waitForAll(anyObject(), anyString())).thenReturn(UUID.randomUUID().toString());
    setInternalState(metricNameCollectionJob, "delegateService", delegateService);
    setInternalState(metricNameCollectionJob, "waitNotifyEngine", waitNotifyEngine);
  }

  @Test
  public void testCompare() {
    NewRelicMetricAnalysis analysis1 =
        NewRelicMetricAnalysis.builder().metricName("metric1").riskLevel(RiskLevel.HIGH).build();
    NewRelicMetricAnalysis analysis2 =
        NewRelicMetricAnalysis.builder().metricName("metric0").riskLevel(RiskLevel.MEDIUM).build();

    assertTrue(analysis1.compareTo(analysis2) < 0);
    TreeSet<NewRelicMetricAnalysis> treeSet = new TreeSet<>();
    treeSet.add(analysis2);
    treeSet.add(analysis1);

    assertEquals(analysis1, treeSet.first());

    analysis2.setRiskLevel(RiskLevel.HIGH);
    assertTrue(analysis1.compareTo(analysis2) > 0);

    treeSet.clear();
    treeSet.add(analysis1);
    treeSet.add(analysis2);
    assertEquals(analysis2, treeSet.first());

    analysis1.setMetricName("metric0");
    assertTrue(analysis1.compareTo(analysis2) == 0);

    NewRelicMetricAnalysis analysis3 =
        NewRelicMetricAnalysis.builder().metricName("abc").riskLevel(RiskLevel.HIGH).build();
    assertTrue(analysis3.compareTo(analysis1) < 0);
  }

  @Test
  public void testMetricNameAlertOpen() {
    NewRelicConfig newRelicConfig1 = NewRelicConfig.builder()
                                         .accountId(accountId)
                                         .apiKey(UUID.randomUUID().toString().toCharArray())
                                         .newRelicUrl(UUID.randomUUID().toString())
                                         .build();
    SettingAttribute settingAttribute1 = aSettingAttribute()
                                             .withAccountId(accountId)
                                             .withName(UUID.randomUUID().toString())
                                             .withValue(newRelicConfig1)
                                             .build();
    String newRelicConfigId1 = wingsPersistence.save(settingAttribute1);
    NewRelicMetricNames metricName1 =
        NewRelicMetricNames.builder()
            .newRelicAppId(String.valueOf(new Random().nextInt()))
            .newRelicConfigId(newRelicConfigId1)
            .lastUpdatedTime(System.currentTimeMillis() - NEW_RELIC_METRIC_COLLECTION_ALERT_THRESHOLD)
            .metrics(Lists.newArrayList(NewRelicMetric.builder().name(UUID.randomUUID().toString()).build(),
                NewRelicMetric.builder().name(UUID.randomUUID().toString()).build()))
            .registeredWorkflows(Lists.newArrayList(WorkflowInfo.builder()
                                                        .accountId(accountId)
                                                        .appId(appId)
                                                        .workflowId(UUID.randomUUID().toString())
                                                        .build(),
                WorkflowInfo.builder()
                    .accountId(accountId)
                    .appId(appId)
                    .workflowId(UUID.randomUUID().toString())
                    .build()))
            .build();
    NewRelicConfig newRelicConfig2 = NewRelicConfig.builder()
                                         .accountId(accountId)
                                         .apiKey(UUID.randomUUID().toString().toCharArray())
                                         .newRelicUrl(UUID.randomUUID().toString())
                                         .build();
    SettingAttribute settingAttribute2 = aSettingAttribute()
                                             .withAccountId(accountId)
                                             .withName(UUID.randomUUID().toString())
                                             .withValue(newRelicConfig2)
                                             .build();
    String newRelicConfigId2 = wingsPersistence.save(settingAttribute2);
    NewRelicMetricNames metricName2 =
        NewRelicMetricNames.builder()
            .newRelicAppId(String.valueOf(new Random().nextInt()))
            .newRelicConfigId(newRelicConfigId2)
            .lastUpdatedTime(System.currentTimeMillis() - NEW_RELIC_METRIC_COLLECTION_ALERT_THRESHOLD)
            .metrics(Lists.newArrayList(NewRelicMetric.builder().name(UUID.randomUUID().toString()).build(),
                NewRelicMetric.builder().name(UUID.randomUUID().toString()).build()))
            .registeredWorkflows(Lists.newArrayList(WorkflowInfo.builder()
                                                        .accountId(accountId)
                                                        .appId(appId)
                                                        .workflowId(UUID.randomUUID().toString())
                                                        .build(),
                WorkflowInfo.builder()
                    .accountId(accountId)
                    .appId(appId)
                    .workflowId(UUID.randomUUID().toString())
                    .build()))
            .build();
    when(metricDataAnalysisService.listMetricNamesWithWorkflows())
        .thenReturn(Lists.newArrayList(metricName1, metricName2));
    setInternalState(metricNameCollectionJob, "metricDataAnalysisService", metricDataAnalysisService);
    assertTrue(wingsPersistence.createQuery(Alert.class).asList().isEmpty());
    metricNameCollectionJob.execute(null);
    List<Alert> alerts = wingsPersistence.createQuery(Alert.class).asList();

    assertEquals(2, alerts.size());
    Alert alert = alerts.get(0);
    assertEquals(AlertType.NEW_RELIC_METRIC_NAMES_COLLECTION, alert.getType());
    assertEquals(AlertStatus.Open, alert.getStatus());
    assertEquals(AlertCategory.Setup, alert.getCategory());
    assertEquals(AlertSeverity.Error, alert.getSeverity());

    NewRelicMetricNameCollectionAlert alertData = (NewRelicMetricNameCollectionAlert) alert.getAlertData();
    assertEquals(newRelicConfigId1, alertData.getConfigId());
    assertEquals(
        "NewRelic metric name collection task past due over 6 hours for connector " + settingAttribute1.getName(),
        alertData.getMessage());

    alert = alerts.get(1);
    assertEquals(AlertType.NEW_RELIC_METRIC_NAMES_COLLECTION, alert.getType());
    assertEquals(AlertStatus.Open, alert.getStatus());
    assertEquals(AlertCategory.Setup, alert.getCategory());
    assertEquals(AlertSeverity.Error, alert.getSeverity());

    alertData = (NewRelicMetricNameCollectionAlert) alert.getAlertData();
    assertEquals(newRelicConfigId2, alertData.getConfigId());
    assertEquals(
        "NewRelic metric name collection task past due over 6 hours for connector " + settingAttribute2.getName(),
        alertData.getMessage());
  }

  @Test
  public void testMetricNameAlertClose() {
    NewRelicConfig newRelicConfig1 = NewRelicConfig.builder()
                                         .accountId(accountId)
                                         .apiKey(UUID.randomUUID().toString().toCharArray())
                                         .newRelicUrl(UUID.randomUUID().toString())
                                         .build();
    SettingAttribute settingAttribute1 = aSettingAttribute()
                                             .withAccountId(accountId)
                                             .withName(UUID.randomUUID().toString())
                                             .withValue(newRelicConfig1)
                                             .build();
    String newRelicConfigId1 = wingsPersistence.save(settingAttribute1);
    NewRelicMetricNames metricName1 =
        NewRelicMetricNames.builder()
            .newRelicAppId(String.valueOf(new Random().nextInt()))
            .newRelicConfigId(newRelicConfigId1)
            .lastUpdatedTime(System.currentTimeMillis() - NEW_RELIC_METRIC_COLLECTION_ALERT_THRESHOLD)
            .metrics(Lists.newArrayList(NewRelicMetric.builder().name(UUID.randomUUID().toString()).build(),
                NewRelicMetric.builder().name(UUID.randomUUID().toString()).build()))
            .registeredWorkflows(Lists.newArrayList(WorkflowInfo.builder()
                                                        .accountId(accountId)
                                                        .appId(appId)
                                                        .workflowId(UUID.randomUUID().toString())
                                                        .build(),
                WorkflowInfo.builder()
                    .accountId(accountId)
                    .appId(appId)
                    .workflowId(UUID.randomUUID().toString())
                    .build()))
            .build();
    NewRelicConfig newRelicConfig2 = NewRelicConfig.builder()
                                         .accountId(accountId)
                                         .apiKey(UUID.randomUUID().toString().toCharArray())
                                         .newRelicUrl(UUID.randomUUID().toString())
                                         .build();
    SettingAttribute settingAttribute2 = aSettingAttribute()
                                             .withAccountId(accountId)
                                             .withName(UUID.randomUUID().toString())
                                             .withValue(newRelicConfig2)
                                             .build();
    String newRelicConfigId2 = wingsPersistence.save(settingAttribute2);
    NewRelicMetricNames metricName2 =
        NewRelicMetricNames.builder()
            .newRelicAppId(String.valueOf(new Random().nextInt()))
            .newRelicConfigId(newRelicConfigId2)
            .lastUpdatedTime(System.currentTimeMillis() - NEW_RELIC_METRIC_COLLECTION_ALERT_THRESHOLD)
            .metrics(Lists.newArrayList(NewRelicMetric.builder().name(UUID.randomUUID().toString()).build(),
                NewRelicMetric.builder().name(UUID.randomUUID().toString()).build()))
            .registeredWorkflows(Lists.newArrayList(WorkflowInfo.builder()
                                                        .accountId(accountId)
                                                        .appId(appId)
                                                        .workflowId(UUID.randomUUID().toString())
                                                        .build(),
                WorkflowInfo.builder()
                    .accountId(accountId)
                    .appId(appId)
                    .workflowId(UUID.randomUUID().toString())
                    .build()))
            .build();
    when(metricDataAnalysisService.listMetricNamesWithWorkflows())
        .thenReturn(Lists.newArrayList(metricName1, metricName2));
    setInternalState(metricNameCollectionJob, "metricDataAnalysisService", metricDataAnalysisService);
    assertTrue(wingsPersistence.createQuery(Alert.class).asList().isEmpty());
    metricNameCollectionJob.execute(null);
    List<Alert> alerts = wingsPersistence.createQuery(Alert.class).asList();

    assertEquals(2, alerts.size());
    Alert alert = alerts.get(0);
    assertEquals(AlertType.NEW_RELIC_METRIC_NAMES_COLLECTION, alert.getType());
    assertEquals(AlertStatus.Open, alert.getStatus());
    assertEquals(AlertCategory.Setup, alert.getCategory());
    assertEquals(AlertSeverity.Error, alert.getSeverity());

    NewRelicMetricNameCollectionAlert alertData = (NewRelicMetricNameCollectionAlert) alert.getAlertData();
    assertEquals(newRelicConfigId1, alertData.getConfigId());
    assertEquals(
        "NewRelic metric name collection task past due over 6 hours for connector " + settingAttribute1.getName(),
        alertData.getMessage());

    alert = alerts.get(1);
    assertEquals(AlertType.NEW_RELIC_METRIC_NAMES_COLLECTION, alert.getType());
    assertEquals(AlertStatus.Open, alert.getStatus());
    assertEquals(AlertCategory.Setup, alert.getCategory());
    assertEquals(AlertSeverity.Error, alert.getSeverity());

    alertData = (NewRelicMetricNameCollectionAlert) alert.getAlertData();
    assertEquals(newRelicConfigId2, alertData.getConfigId());
    assertEquals(
        "NewRelic metric name collection task past due over 6 hours for connector " + settingAttribute2.getName(),
        alertData.getMessage());

    metricName1.setLastUpdatedTime(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));

    metricNameCollectionJob.execute(null);
    alerts = wingsPersistence.createQuery(Alert.class).asList();

    assertEquals(2, alerts.size());
    alert = alerts.get(0);
    assertEquals(AlertType.NEW_RELIC_METRIC_NAMES_COLLECTION, alert.getType());
    assertEquals(AlertStatus.Closed, alert.getStatus());
    assertEquals(AlertCategory.Setup, alert.getCategory());
    assertEquals(AlertSeverity.Error, alert.getSeverity());

    alertData = (NewRelicMetricNameCollectionAlert) alert.getAlertData();
    assertEquals(newRelicConfigId1, alertData.getConfigId());
    assertEquals(
        "NewRelic metric name collection task past due over 6 hours for connector " + settingAttribute1.getName(),
        alertData.getMessage());

    alert = alerts.get(1);
    assertEquals(AlertType.NEW_RELIC_METRIC_NAMES_COLLECTION, alert.getType());
    assertEquals(AlertStatus.Open, alert.getStatus());
    assertEquals(AlertCategory.Setup, alert.getCategory());
    assertEquals(AlertSeverity.Error, alert.getSeverity());

    alertData = (NewRelicMetricNameCollectionAlert) alert.getAlertData();
    assertEquals(newRelicConfigId2, alertData.getConfigId());
    assertEquals(
        "NewRelic metric name collection task past due over 6 hours for connector " + settingAttribute2.getName(),
        alertData.getMessage());

    metricName2.setLastUpdatedTime(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));

    metricNameCollectionJob.execute(null);
    alerts = wingsPersistence.createQuery(Alert.class).asList();

    assertEquals(2, alerts.size());
    alert = alerts.get(0);
    assertEquals(AlertType.NEW_RELIC_METRIC_NAMES_COLLECTION, alert.getType());
    assertEquals(AlertStatus.Closed, alert.getStatus());
    assertEquals(AlertCategory.Setup, alert.getCategory());
    assertEquals(AlertSeverity.Error, alert.getSeverity());

    alertData = (NewRelicMetricNameCollectionAlert) alert.getAlertData();
    assertEquals(newRelicConfigId1, alertData.getConfigId());
    assertEquals(
        "NewRelic metric name collection task past due over 6 hours for connector " + settingAttribute1.getName(),
        alertData.getMessage());

    alert = alerts.get(1);
    assertEquals(AlertType.NEW_RELIC_METRIC_NAMES_COLLECTION, alert.getType());
    assertEquals(AlertStatus.Closed, alert.getStatus());
    assertEquals(AlertCategory.Setup, alert.getCategory());
    assertEquals(AlertSeverity.Error, alert.getSeverity());

    alertData = (NewRelicMetricNameCollectionAlert) alert.getAlertData();
    assertEquals(newRelicConfigId2, alertData.getConfigId());
    assertEquals(
        "NewRelic metric name collection task past due over 6 hours for connector " + settingAttribute2.getName(),
        alertData.getMessage());
  }
}
