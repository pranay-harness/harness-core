package software.wings.service.impl.analysis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static software.wings.common.VerificationConstants.DELAY_MINUTES;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.event.handler.impl.notifications.AlertNotificationHandler;
import io.harness.event.model.EventType;
import io.harness.event.model.GenericEvent;
import io.harness.notifications.AlertNotificationRuleChecker;
import io.harness.rest.RestResponse;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.alerts.AlertCategory;
import software.wings.api.ExecutionDataValue;
import software.wings.beans.FeatureName;
import software.wings.beans.Notification;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;
import software.wings.resources.ContinuousVerificationResource;
import software.wings.service.impl.event.GenericEventListener;
import software.wings.service.impl.event.GenericEventPublisher;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.intfc.AlertNotificationRuleService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.NotificationDispatcherService;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.Builder;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.StateType;
import software.wings.verification.VerificationStateAnalysisExecutionData;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ContinuousVerificationServiceTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String envId;
  private String serviceId;
  private List<Notification> notifications = new ArrayList<>();

  @Inject private ContinuousVerificationService continuousVerificationService;
  @Inject private ContinuousVerificationResource continuousVerificationResource;
  @Inject private GenericEventListener genericEventListener;
  @Inject private GenericEventPublisher genericEventPublisher;
  @Inject private AlertService alertService;
  @Inject private AlertNotificationRuleChecker ruleChecker;
  @Inject private AlertNotificationRuleService ruleService;
  private AlertNotificationHandler alertNotificationHandler;
  @Mock private NotificationDispatcherService notificationDispatcher;
  @Mock private FeatureFlagService featureFlagService;

  @Before
  public void setUp() throws Exception {
    accountId = generateUuid();
    appId = generateUuid();
    envId = generateUuid();
    serviceId = generateUuid();
    doAnswer(invocationOnMock -> notifications.add((Notification) invocationOnMock.getArguments()[0]))
        .when(notificationDispatcher)
        .dispatch(any(), any());

    writeField(alertService, "eventPublisher", genericEventPublisher, true);
    writeField(continuousVerificationService, "alertService", alertService, true);
    writeField(continuousVerificationService, "featureFlagService", featureFlagService, true);
    alertNotificationHandler = new AlertNotificationHandler(genericEventListener);
    writeField(alertNotificationHandler, "notificationDispatcher", notificationDispatcher, true);
    writeField(alertNotificationHandler, "ruleChecker", ruleChecker, true);
    writeField(alertNotificationHandler, "ruleService", ruleService, true);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetVerificationStateExecutionData() {
    assertThat(continuousVerificationService.getVerificationStateExecutionData(generateUuid())).isNull();
    String stateExecutionId = wingsPersistence.save(Builder.aStateExecutionInstance().build());
    assertThat(continuousVerificationService.getVerificationStateExecutionData(stateExecutionId)).isNull();

    when(featureFlagService.isEnabled(any(FeatureName.class), anyString())).thenReturn(false);

    final String displayName = "new relic";
    Map<String, StateExecutionData> stateExecutionMap = new HashMap<>();
    final VerificationStateAnalysisExecutionData verificationStateAnalysisExecutionData =
        VerificationStateAnalysisExecutionData.builder()
            .stateExecutionInstanceId(stateExecutionId)
            .canaryNewHostNames(Sets.newHashSet("host1", "host2", "controlNode-1", "controlNode-2", "testNode-1"))
            .lastExecutionNodes(Sets.newHashSet("host3", "host4", "controlNode-3", "controlNode-4", "testNode-2"))
            .query(generateUuid())
            .baselineExecutionId(generateUuid())
            .correlationId(generateUuid())
            .analysisMinute(5)
            .build();
    stateExecutionMap.put(displayName, verificationStateAnalysisExecutionData);

    wingsPersistence.updateField(StateExecutionInstance.class, stateExecutionId,
        StateExecutionInstanceKeys.stateExecutionMap, stateExecutionMap);
    wingsPersistence.updateField(
        StateExecutionInstance.class, stateExecutionId, StateExecutionInstanceKeys.displayName, displayName);
    final AnalysisContext analysisContext =
        AnalysisContext.builder().timeDuration(8).stateExecutionId(stateExecutionId).build();
    wingsPersistence.save(analysisContext);

    VerificationStateAnalysisExecutionData verificationStateExecutionData =
        continuousVerificationService.getVerificationStateExecutionData(stateExecutionId);
    assertThat(verificationStateExecutionData).isNotNull();
    assertThat(verificationStateExecutionData.getStateExecutionInstanceId())
        .isEqualTo(verificationStateAnalysisExecutionData.getStateExecutionInstanceId());
    assertThat(verificationStateExecutionData.getCanaryNewHostNames())
        .isEqualTo(verificationStateAnalysisExecutionData.getCanaryNewHostNames());
    assertThat(verificationStateExecutionData.getLastExecutionNodes())
        .isEqualTo(verificationStateAnalysisExecutionData.getLastExecutionNodes());
    assertThat(verificationStateExecutionData.getQuery()).isEqualTo(verificationStateAnalysisExecutionData.getQuery());
    assertThat(verificationStateExecutionData.getBaselineExecutionId())
        .isEqualTo(verificationStateAnalysisExecutionData.getBaselineExecutionId());
    assertThat(verificationStateExecutionData.getAnalysisMinute())
        .isEqualTo(verificationStateAnalysisExecutionData.getAnalysisMinute());
    assertThat(verificationStateExecutionData.getProgressPercentage()).isEqualTo(0);
    assertThat(verificationStateExecutionData.getRemainingMinutes())
        .isEqualTo(analysisContext.getTimeDuration() + DELAY_MINUTES + 1);

    // test one record
    NewRelicMetricAnalysisRecord newRelicMetricAnalysisRecord1 = new NewRelicMetricAnalysisRecord();
    newRelicMetricAnalysisRecord1.setStateExecutionId(stateExecutionId);
    newRelicMetricAnalysisRecord1.setAnalysisMinute(1);
    newRelicMetricAnalysisRecord1.setCreatedAt(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(8));
    wingsPersistence.save(newRelicMetricAnalysisRecord1);

    verificationStateExecutionData = continuousVerificationService.getVerificationStateExecutionData(stateExecutionId);
    assertThat(verificationStateExecutionData.getProgressPercentage())
        .isEqualTo(100 / analysisContext.getTimeDuration());
    assertThat(verificationStateExecutionData.getRemainingMinutes())
        .isEqualTo(analysisContext.getTimeDuration() + DELAY_MINUTES);

    // test two records
    NewRelicMetricAnalysisRecord newRelicMetricAnalysisRecord2 = new NewRelicMetricAnalysisRecord();
    newRelicMetricAnalysisRecord2.setStateExecutionId(stateExecutionId);
    newRelicMetricAnalysisRecord2.setAnalysisMinute(2);
    newRelicMetricAnalysisRecord2.setCreatedAt(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(2));
    wingsPersistence.save(newRelicMetricAnalysisRecord2);

    verificationStateExecutionData = continuousVerificationService.getVerificationStateExecutionData(stateExecutionId);
    assertThat(verificationStateExecutionData.getProgressPercentage())
        .isEqualTo(100 * 2 / analysisContext.getTimeDuration());
    assertThat(verificationStateExecutionData.getRemainingMinutes())
        .isEqualTo(TimeUnit.MILLISECONDS.toMinutes((analysisContext.getTimeDuration() - 2)
            * ((newRelicMetricAnalysisRecord2.getCreatedAt() - newRelicMetricAnalysisRecord1.getCreatedAt()) / 2)));

    wingsPersistence.delete(wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class, excludeAuthority));

    // test one record
    TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord1 = new TimeSeriesMLAnalysisRecord();
    timeSeriesMLAnalysisRecord1.setStateExecutionId(stateExecutionId);
    timeSeriesMLAnalysisRecord1.setAnalysisMinute(1);
    timeSeriesMLAnalysisRecord1.setCreatedAt(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(12));
    wingsPersistence.save(timeSeriesMLAnalysisRecord1);

    verificationStateExecutionData = continuousVerificationService.getVerificationStateExecutionData(stateExecutionId);
    assertThat(verificationStateExecutionData.getProgressPercentage())
        .isEqualTo(100 / analysisContext.getTimeDuration());
    assertThat(verificationStateExecutionData.getRemainingMinutes())
        .isEqualTo(analysisContext.getTimeDuration() + DELAY_MINUTES);

    // test two records
    TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord2 = new TimeSeriesMLAnalysisRecord();
    timeSeriesMLAnalysisRecord2.setStateExecutionId(stateExecutionId);
    timeSeriesMLAnalysisRecord2.setCreatedAt(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(2));
    timeSeriesMLAnalysisRecord2.setAnalysisMinute(2);
    wingsPersistence.save(timeSeriesMLAnalysisRecord2);

    verificationStateExecutionData = continuousVerificationService.getVerificationStateExecutionData(stateExecutionId);
    assertThat(verificationStateExecutionData.getProgressPercentage())
        .isEqualTo(100 * 2 / analysisContext.getTimeDuration());
    assertThat(verificationStateExecutionData.getRemainingMinutes())
        .isEqualTo(TimeUnit.MILLISECONDS.toMinutes((analysisContext.getTimeDuration() - 2)
            * ((timeSeriesMLAnalysisRecord2.getCreatedAt() - timeSeriesMLAnalysisRecord1.getCreatedAt()) / 2)));

    // test the nodes
    final Map<String, ExecutionDataValue> executionSummary =
        verificationStateAnalysisExecutionData.getExecutionSummary();
    assertThat(executionSummary.size()).isGreaterThan(0);
    assertThat(executionSummary.get("newVersionNodes").getValue()).isEqualTo(Sets.newHashSet("host1", "host2"));
    assertThat(executionSummary.get("previousVersionNodes").getValue()).isEqualTo(Sets.newHashSet("host3", "host4"));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCVCertifiedDetailsForWorkflow() {
    String accountId = generateUuid();
    String appId = generateUuid();
    String stateExecutionId1 = wingsPersistence.save(Builder.aStateExecutionInstance()
                                                         .stateType(StateType.ELK.name())
                                                         .appId(appId)
                                                         .status(ExecutionStatus.SUCCESS)
                                                         .build());
    String stateExecutionId2 = wingsPersistence.save(Builder.aStateExecutionInstance()
                                                         .stateType(StateType.NEW_RELIC.name())
                                                         .appId(appId)
                                                         .status(ExecutionStatus.FAILED)
                                                         .build());

    String workflowExecutionId = generateUuid();
    wingsPersistence.save(ContinuousVerificationExecutionMetaData.builder()
                              .accountId(accountId)
                              .stateExecutionId(stateExecutionId1)
                              .workflowExecutionId(workflowExecutionId)
                              .build());
    wingsPersistence.save(ContinuousVerificationExecutionMetaData.builder()
                              .accountId(accountId)
                              .stateExecutionId(stateExecutionId2)
                              .workflowExecutionId(workflowExecutionId)
                              .build());

    List<CVCertifiedDetailsForWorkflowState> stateExecutionInstances =
        continuousVerificationService.getCVCertifiedDetailsForWorkflow(accountId, appId, workflowExecutionId);
    assertThat(stateExecutionInstances).isNotEmpty();
    assertThat(stateExecutionInstances.size()).isEqualTo(2);
    List<String> states = Arrays.asList("ELK", "NEW_RELIC");
    assertThat(stateExecutionInstances.get(0).getExecutionDetails().getStateType()).isIn(states);
    assertThat(stateExecutionInstances.get(1).getExecutionDetails().getStateType()).isIn(states);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCVCertifiedDetailsForWorkflowFromResource() {
    String accountId = generateUuid();
    String appId = generateUuid();
    String stateExecutionId1 = wingsPersistence.save(Builder.aStateExecutionInstance()
                                                         .stateType(StateType.ELK.name())
                                                         .appId(appId)
                                                         .status(ExecutionStatus.SUCCESS)
                                                         .build());
    String stateExecutionId2 = wingsPersistence.save(Builder.aStateExecutionInstance()
                                                         .stateType(StateType.NEW_RELIC.name())
                                                         .appId(appId)
                                                         .status(ExecutionStatus.FAILED)
                                                         .build());

    String workflowExecutionId = generateUuid();
    wingsPersistence.save(ContinuousVerificationExecutionMetaData.builder()
                              .accountId(accountId)
                              .stateExecutionId(stateExecutionId1)
                              .workflowExecutionId(workflowExecutionId)
                              .build());
    wingsPersistence.save(ContinuousVerificationExecutionMetaData.builder()
                              .accountId(accountId)
                              .stateExecutionId(stateExecutionId2)
                              .workflowExecutionId(workflowExecutionId)
                              .build());

    RestResponse<List<CVCertifiedDetailsForWorkflowState>> result =
        continuousVerificationResource.getCVCertifiedLabelsForWorkflow(accountId, appId, workflowExecutionId);
    List<CVCertifiedDetailsForWorkflowState> stateExecutionInstances = result.getResource();
    assertThat(stateExecutionInstances).isNotEmpty();
    assertThat(stateExecutionInstances.size()).isEqualTo(2);
    List<String> states = Arrays.asList("ELK", "NEW_RELIC");
    assertThat(stateExecutionInstances.get(0).getExecutionDetails().getStateType()).isIn(states);
    assertThat(stateExecutionInstances.get(1).getExecutionDetails().getStateType()).isIn(states);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCVCertifiedDetailsForPipeline() {
    String accountId = generateUuid();
    String appId = generateUuid();
    String stateExecutionId1 = wingsPersistence.save(Builder.aStateExecutionInstance()
                                                         .stateType(StateType.SPLUNKV2.name())
                                                         .appId(appId)
                                                         .status(ExecutionStatus.SUCCESS)
                                                         .build());
    String stateExecutionId2 = wingsPersistence.save(Builder.aStateExecutionInstance()
                                                         .stateType(StateType.APP_DYNAMICS.name())
                                                         .status(ExecutionStatus.FAILED)
                                                         .appId(appId)
                                                         .build());

    String workflowExecutionId = generateUuid(), pipelineExecutionId = generateUuid();
    wingsPersistence.save(ContinuousVerificationExecutionMetaData.builder()
                              .accountId(accountId)
                              .stateExecutionId(stateExecutionId1)
                              .pipelineExecutionId(pipelineExecutionId)
                              .workflowExecutionId(workflowExecutionId)
                              .phaseName("Phase 1")
                              .build());
    wingsPersistence.save(ContinuousVerificationExecutionMetaData.builder()
                              .accountId(accountId)
                              .stateExecutionId(stateExecutionId2)
                              .pipelineExecutionId(pipelineExecutionId)
                              .workflowExecutionId(workflowExecutionId)
                              .phaseName("Phase 1")
                              .build());

    List<CVCertifiedDetailsForWorkflowState> stateExecutionInstances =
        continuousVerificationService.getCVCertifiedDetailsForPipeline(accountId, appId, pipelineExecutionId);
    List<String> states = Arrays.asList("SPLUNKV2", "APP_DYNAMICS");
    assertThat(stateExecutionInstances).isNotEmpty();
    assertThat(stateExecutionInstances.size()).isEqualTo(2);
    assertThat(stateExecutionInstances.get(0).getExecutionDetails().getStateType()).isIn(states);
    assertThat(stateExecutionInstances.get(1).getExecutionDetails().getStateType()).isIn(states);
    assertThat(stateExecutionInstances.get(0).getPhaseName()).isEqualTo("Phase 1");
    assertThat(stateExecutionInstances.get(1).getPhaseName()).isEqualTo("Phase 1");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCVCertifiedDetailsForPipelineFromResource() {
    String accountId = generateUuid();
    String appId = generateUuid();
    String stateExecutionId1 = wingsPersistence.save(Builder.aStateExecutionInstance()
                                                         .stateType(StateType.SPLUNKV2.name())
                                                         .appId(appId)
                                                         .status(ExecutionStatus.SUCCESS)
                                                         .build());
    String stateExecutionId2 = wingsPersistence.save(Builder.aStateExecutionInstance()
                                                         .stateType(StateType.APP_DYNAMICS.name())
                                                         .status(ExecutionStatus.FAILED)
                                                         .appId(appId)
                                                         .build());

    String workflowExecutionId = generateUuid(), pipelineExecutionId = generateUuid();
    wingsPersistence.save(ContinuousVerificationExecutionMetaData.builder()
                              .accountId(accountId)
                              .stateExecutionId(stateExecutionId1)
                              .pipelineExecutionId(pipelineExecutionId)
                              .workflowExecutionId(workflowExecutionId)
                              .phaseName("Phase 1")
                              .build());
    wingsPersistence.save(ContinuousVerificationExecutionMetaData.builder()
                              .accountId(accountId)
                              .stateExecutionId(stateExecutionId2)
                              .pipelineExecutionId(pipelineExecutionId)
                              .workflowExecutionId(workflowExecutionId)
                              .phaseName("Phase 1")
                              .build());

    RestResponse<List<CVCertifiedDetailsForWorkflowState>> stateExecutionInstances =
        continuousVerificationResource.getCVCertifiedLabelsForPipeline(accountId, appId, pipelineExecutionId);
    List<CVCertifiedDetailsForWorkflowState> result = stateExecutionInstances.getResource();
    List<String> states = Arrays.asList("SPLUNKV2", "APP_DYNAMICS");
    assertThat(result).isNotEmpty();
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get(0).getExecutionDetails().getStateType()).isIn(states);
    assertThat(result.get(1).getExecutionDetails().getStateType()).isIn(states);
    assertThat(result.get(0).getPhaseName()).isEqualTo("Phase 1");
    assertThat(result.get(1).getPhaseName()).isEqualTo("Phase 1");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCVCertifiedDetailsForPipelineNoCVStates() {
    String accountId = generateUuid();
    String appId = generateUuid();
    String workflowExecutionId = generateUuid(), pipelineExecutionId = generateUuid();
    List<CVCertifiedDetailsForWorkflowState> stateExecutionInstances =
        continuousVerificationService.getCVCertifiedDetailsForPipeline(accountId, appId, pipelineExecutionId);
    assertThat(stateExecutionInstances).isEmpty();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCVCertifiedDetailsForWorkflowNoCVStates() {
    String accountId = generateUuid();
    String appId = generateUuid();
    String workflowExecutionId = generateUuid(), pipelineExecutionId = generateUuid();
    List<CVCertifiedDetailsForWorkflowState> stateExecutionInstances =
        continuousVerificationService.getCVCertifiedDetailsForWorkflow(accountId, appId, workflowExecutionId);
    assertThat(stateExecutionInstances).isEmpty();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testNotification() {
    final NewRelicCVServiceConfiguration cvConfig = createCvConfig();
    final String cvConfigId = wingsPersistence.save(cvConfig);

    wingsPersistence.save(
        new AlertNotificationRule(accountId, AlertCategory.ContinuousVerification, null, Sets.newHashSet()));
    continuousVerificationService.openAlert(cvConfigId,
        ContinuousVerificationAlertData.builder()
            .cvConfiguration(cvConfig)
            .analysisStartTime(10)
            .analysisStartTime(25)
            .riskScore(0.6)
            .mlAnalysisType(MLAnalysisType.TIME_SERIES)
            .accountId(accountId)
            .build());
    waitForAlertEvent(1);
    List<GenericEvent> genericEvents = wingsPersistence.createQuery(GenericEvent.class, excludeAuthority).asList();
    assertThat(genericEvents.size()).isEqualTo(1);
    GenericEvent genericEvent = genericEvents.get(0);
    assertThat(genericEvent.getEvent().getEventType()).isEqualTo(EventType.OPEN_ALERT);

    alertNotificationHandler.handleEvent(genericEvent.getEvent());
    assertThat(notifications.size()).isEqualTo(1);
    assertThat(notifications.get(0).getEventType()).isEqualTo(EventType.OPEN_ALERT);

    // now close the alert and ensure that the notficiation comes
    continuousVerificationService.closeAlert(cvConfigId,
        ContinuousVerificationAlertData.builder()
            .cvConfiguration(cvConfig)
            .analysisStartTime(35)
            .analysisStartTime(50)
            .riskScore(0.2)
            .mlAnalysisType(MLAnalysisType.TIME_SERIES)
            .accountId(accountId)
            .build());

    waitForAlertEvent(2);
    genericEvents = wingsPersistence.createQuery(GenericEvent.class, excludeAuthority).asList();
    assertThat(genericEvents.size()).isEqualTo(2);
    genericEvent = genericEvents.get(1);
    assertThat(genericEvent.getEvent().getEventType()).isEqualTo(EventType.CLOSE_ALERT);

    alertNotificationHandler.handleEvent(genericEvent.getEvent());
    assertThat(notifications.size()).isEqualTo(2);
    assertThat(notifications.get(0).getEventType()).isEqualTo(EventType.OPEN_ALERT);
    assertThat(notifications.get(1).getEventType()).isEqualTo(EventType.CLOSE_ALERT);
  }

  private void waitForAlertEvent(int expectedNumOfEvents) {
    int tryCount = 0;
    List<GenericEvent> alerts;
    do {
      alerts = wingsPersistence.createQuery(GenericEvent.class, excludeAuthority).asList();
      tryCount++;
      sleep(ofMillis(500));
    } while (alerts.size() < expectedNumOfEvents && tryCount < 10);
  }

  private NewRelicCVServiceConfiguration createCvConfig() {
    final NewRelicCVServiceConfiguration cvConfiguration = new NewRelicCVServiceConfiguration();
    cvConfiguration.setAppId(appId);
    cvConfiguration.setEnvId(envId);
    cvConfiguration.setServiceId(serviceId);
    cvConfiguration.setAlertEnabled(false);
    cvConfiguration.setAlertThreshold(0.5);
    cvConfiguration.setName(generateUuid());
    cvConfiguration.setAccountId(accountId);
    return cvConfiguration;
  }
}
