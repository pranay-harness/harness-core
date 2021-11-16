package software.wings.service.delegate;

import static io.harness.beans.DelegateTask.Status.ABORTED;
import static io.harness.beans.DelegateTask.Status.ERROR;
import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.beans.DelegateTask.Status.STARTED;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.DelegateTaskAbortEvent.Builder.aDelegateTaskAbortEvent;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.BOJAN;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.MARKOM;
import static io.harness.rule.OwnerRule.PUNEET;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.VUK;

import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.service.impl.DelegateServiceImpl.TASK_CATEGORY_MAP;
import static software.wings.service.impl.DelegateServiceImpl.TASK_SELECTORS;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_TEMPLATE_ID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.Cd1SetupFields;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.FeatureName;
import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.CapabilityTaskSelectionDetails;
import io.harness.capability.HttpConnectionParameters;
import io.harness.capability.service.CapabilityService;
import io.harness.category.element.UnitTests;
import io.harness.configuration.DeployMode;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateBuilder;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskEvent;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskRank;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.DelegateTaskResponse.ResponseCode;
import io.harness.delegate.beans.FileUploadLimit;
import io.harness.delegate.beans.NoAvailableDelegatesException;
import io.harness.delegate.beans.NoInstalledDelegatesException;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.TaskSelectorMap;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.executioncapability.BatchCapabilityCheckTaskParameters;
import io.harness.delegate.task.executioncapability.BatchCapabilityCheckTaskResponse;
import io.harness.delegate.task.executioncapability.CapabilityCheckDetails;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.logstreaming.LogStreamingServiceConfig;
import io.harness.network.LocalhostUtils;
import io.harness.observer.Subject;
import io.harness.persistence.HPersistence;
import io.harness.rule.Cache;
import io.harness.rule.Owner;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateCache;
import io.harness.service.intfc.DelegateProfileObserver;
import io.harness.service.intfc.DelegateSyncService;
import io.harness.service.intfc.DelegateTaskRetryObserver;
import io.harness.service.intfc.DelegateTaskService;
import io.harness.version.VersionInfo;
import io.harness.version.VersionInfoManager;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.FeatureTestHelper;
import software.wings.WingsBaseTest;
import software.wings.app.DelegateGrpcConfig;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.LicenseInfo;
import software.wings.beans.TaskType;
import software.wings.cdn.CdnConfig;
import software.wings.delegatetasks.cv.RateLimitExceededException;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.jre.JreConfig;
import software.wings.service.impl.DelegateObserver;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.DelegateTaskBroadcastHelper;
import software.wings.service.impl.DelegateTaskServiceClassicImpl;
import software.wings.service.impl.DelegateTaskStatusObserver;
import software.wings.service.impl.infra.InfraDownloadService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.sm.ExecutionStatusData;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@BreakDependencyOn("software.wings.WingsBaseTest")
@BreakDependencyOn("software.wings.app.MainConfiguration")
public class DelegateTaskServiceClassicTest extends WingsBaseTest {
  private static final String VERSION = "1.0.0";
  private static final String DELEGATE_TYPE = "dockerType";

  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private AccountService accountService;
  @Mock private MainConfiguration mainConfiguration;
  @Mock private BroadcasterFactory broadcasterFactory;
  @Mock private Broadcaster broadcaster;
  @Mock private AssignDelegateService assignDelegateService;
  @Mock private InfraDownloadService infraDownloadService;
  @Mock private VersionInfoManager versionInfoManager;
  @Mock private SubdomainUrlHelperIntfc subdomainUrlHelper;
  @Mock private DelegateGrpcConfig delegateGrpcConfig;
  @Mock private CapabilityService capabilityService;
  @Mock private DelegateSyncService delegateSyncService;
  @Mock private DelegateSelectionLogsService delegateSelectionLogsService;

  @Inject private FeatureTestHelper featureTestHelper;
  @Inject private KryoSerializer kryoSerializer;

  private int port = LocalhostUtils.findFreePort();
  @Rule public WireMockRule wireMockRule = new WireMockRule(port);
  @Rule public ExpectedException thrown = ExpectedException.none();

  @InjectMocks @Inject private DelegateCache delegateCache;
  @InjectMocks @Inject private DelegateServiceImpl delegateService;
  @InjectMocks @Inject private DelegateTaskServiceClassicImpl delegateTaskServiceClassic;
  @InjectMocks @Inject private DelegateTaskService delegateTaskService;
  @InjectMocks @Inject private DelegateTaskBroadcastHelper broadcastHelper;
  @Inject private HPersistence persistence;

  private final Subject<DelegateProfileObserver> delegateProfileSubject = mock(Subject.class);
  private final Subject<DelegateTaskRetryObserver> retryObserverSubject = mock(Subject.class);
  private final Subject<DelegateTaskStatusObserver> delegateTaskStatusObserverSubject = mock(Subject.class);
  private final Subject<DelegateObserver> subject = mock(Subject.class);

  private final Account account =
      anAccount().withLicenseInfo(LicenseInfo.builder().accountStatus(AccountStatus.ACTIVE).build()).build();

  @Before
  public void setUp() throws IllegalAccessException {
    CdnConfig cdnConfig = new CdnConfig();
    cdnConfig.setUrl("http://localhost:9500");
    when(subdomainUrlHelper.getDelegateMetadataUrl(any(), any(), any()))
        .thenReturn("http://localhost:" + port + "/delegateci.txt");
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES);
    when(mainConfiguration.getKubectlVersion()).thenReturn("v1.12.2");
    when(mainConfiguration.getScmVersion()).thenReturn("542f4642");
    when(mainConfiguration.getOcVersion()).thenReturn("v4.2.16");
    when(mainConfiguration.getCdnConfig()).thenReturn(cdnConfig);
    HashMap<String, JreConfig> jreConfigMap = new HashMap<>();
    jreConfigMap.put("openjdk8u242", getOpenjdkJreConfig());
    when(mainConfiguration.getCurrentJre()).thenReturn("openjdk8u242");
    when(mainConfiguration.getJreConfigs()).thenReturn(jreConfigMap);
    when(subdomainUrlHelper.getWatcherMetadataUrl(any(), any(), any()))
        .thenReturn("http://localhost:" + port + "/watcherci.txt");
    FileUploadLimit fileUploadLimit = new FileUploadLimit();
    fileUploadLimit.setProfileResultLimit(1000000000L);
    when(mainConfiguration.getFileUploadLimits()).thenReturn(fileUploadLimit);

    LogStreamingServiceConfig logSteamingServiceConfig =
        LogStreamingServiceConfig.builder().serviceToken("token").baseUrl("http://localhost:8079").build();
    when(mainConfiguration.getLogStreamingServiceConfig()).thenReturn(logSteamingServiceConfig);

    PortalConfig portalConfig = new PortalConfig();
    portalConfig.setCriticalDelegateTaskRejectAtLimit(100000);
    portalConfig.setJwtNextGenManagerSecret("**********");
    when(mainConfiguration.getPortal()).thenReturn(portalConfig);
    when(delegateGrpcConfig.getPort()).thenReturn(8080);

    when(accountService.getDelegateConfiguration(anyString()))
        .thenReturn(DelegateConfiguration.builder().delegateVersions(singletonList("0.0.0")).build());
    when(accountService.get(ACCOUNT_ID)).thenReturn(account);
    when(infraDownloadService.getDownloadUrlForDelegate(anyString(), any()))
        .thenReturn("http://localhost:" + port + "/builds/9/delegate.jar");
    when(infraDownloadService.getCdnWatcherBaseUrl()).thenReturn("http://localhost:9500/builds");
    wireMockRule.stubFor(get(urlEqualTo("/delegateci.txt"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody("9.9.9 jobs/delegateci/9/delegate.jar")
                                             .withHeader("Content-Type", "text/plain")));

    wireMockRule.stubFor(head(urlEqualTo("/builds/9/delegate.jar")).willReturn(aResponse().withStatus(200)));

    wireMockRule.stubFor(get(urlEqualTo("/watcherci.txt"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody("8.8.8 jobs/deploy-ci-watcher/8/watcher.jar")
                                             .withHeader("Content-Type", "text/plain")));

    when(broadcasterFactory.lookup(anyString(), anyBoolean())).thenReturn(broadcaster);
    when(versionInfoManager.getVersionInfo()).thenReturn(VersionInfo.builder().version(VERSION).build());
    //    when(delegatesFeature.getMaxUsageAllowedForAccount(ACCOUNT_ID)).thenReturn(Integer.MAX_VALUE);

    FieldUtils.writeField(delegateService, "delegateProfileSubject", delegateProfileSubject, true);
    FieldUtils.writeField(delegateService, "subject", subject, true);
    FieldUtils.writeField(delegateTaskService, "retryObserverSubject", retryObserverSubject, true);
    FieldUtils.writeField(
        delegateTaskService, "delegateTaskStatusObserverSubject", delegateTaskStatusObserverSubject, true);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void shouldGetDelegateTaskEvents() {
    String delegateId = generateUuid();
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    List<DelegateTaskEvent> delegateTaskEvents =
        delegateTaskServiceClassic.getDelegateTaskEvents(ACCOUNT_ID, delegateId, false);
    assertThat(delegateTaskEvents).hasSize(1);
    assertThat(delegateTaskEvents.get(0).getDelegateTaskId()).isEqualTo(delegateTask.getUuid());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldGetDelegateTaskEventsRespectingPreAssignedDelegates() {
    featureTestHelper.enableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
    String delegateId = generateUuid();
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    DelegateTask delegateTask2 = saveDelegateTask(true, emptySet(), QUEUED);

    UpdateOperations<DelegateTask> updateOperations = persistence.createUpdateOperations(DelegateTask.class);
    updateOperations.set(DelegateTaskKeys.preAssignedDelegateId, delegateId);
    persistence.update(delegateTask2, updateOperations);

    List<DelegateTaskEvent> delegateTaskEvents =
        delegateTaskServiceClassic.getDelegateTaskEvents(ACCOUNT_ID, delegateId, false);
    assertThat(delegateTaskEvents).hasSize(1);
    assertThat(delegateTaskEvents.get(0).getDelegateTaskId()).isEqualTo(delegateTask2.getUuid());
    featureTestHelper.disableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldSaveDelegateTask() {
    DelegateTask delegateTask =
        DelegateTask.builder()
            .uuid(generateUuid())
            .accountId(ACCOUNT_ID)
            .executionCapabilities(
                asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
                    "http://www.url.com", null)))
            .waitId(generateUuid())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
            .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, ENV_ID)
            .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, INFRA_MAPPING_ID)
            .setupAbstraction(Cd1SetupFields.SERVICE_TEMPLATE_ID_FIELD, SERVICE_TEMPLATE_ID)
            .setupAbstraction(Cd1SetupFields.ARTIFACT_STREAM_ID_FIELD, ARTIFACT_STREAM_ID)
            .version(VERSION)
            .data(TaskData.builder()
                      .async(true)
                      .taskType(TaskType.HTTP.name())
                      .parameters(new Object[] {})
                      .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                      .build())
            .build();
    delegateTaskServiceClassic.queueTask(delegateTask);
    assertThat(persistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.uuid, delegateTask.getUuid()).get())
        .isEqualTo(delegateTask);
    verify(assignDelegateService, never()).getAccountDelegates(delegateTask.getAccountId());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldSaveDelegateTaskAndInvokeCapabilityLogic() {
    featureTestHelper.enableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
    DelegateTask delegateTask =
        DelegateTask.builder()
            .uuid(generateUuid())
            .accountId(ACCOUNT_ID)
            .executionCapabilities(
                asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
                    "http://www.url.com", null)))
            .waitId(generateUuid())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
            .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, ENV_ID)
            .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, INFRA_MAPPING_ID)
            .setupAbstraction(Cd1SetupFields.SERVICE_TEMPLATE_ID_FIELD, SERVICE_TEMPLATE_ID)
            .setupAbstraction(Cd1SetupFields.ARTIFACT_STREAM_ID_FIELD, ARTIFACT_STREAM_ID)
            .version(VERSION)
            .data(TaskData.builder()
                      .async(true)
                      .taskType(TaskType.HTTP.name())
                      .parameters(new Object[] {})
                      .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                      .build())
            .build();
    delegateTaskServiceClassic.queueTask(delegateTask);
    assertThat(persistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.uuid, delegateTask.getUuid()).get())
        .isEqualTo(delegateTask);
    verify(assignDelegateService).getAccountDelegates(delegateTask.getAccountId());
    featureTestHelper.disableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldSaveDelegateTaskWhenRankLimitIsNotReached() {
    DelegateTask delegateTask =
        DelegateTask.builder()
            .uuid(generateUuid())
            .accountId(ACCOUNT_ID)
            .executionCapabilities(
                asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
                    "http://www.url.com", null)))
            .rank(DelegateTaskRank.OPTIONAL)
            .waitId(generateUuid())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
            .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, ENV_ID)
            .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, INFRA_MAPPING_ID)
            .setupAbstraction(Cd1SetupFields.SERVICE_TEMPLATE_ID_FIELD, SERVICE_TEMPLATE_ID)
            .setupAbstraction(Cd1SetupFields.ARTIFACT_STREAM_ID_FIELD, ARTIFACT_STREAM_ID)
            .version(VERSION)
            .data(TaskData.builder()
                      .async(true)
                      .taskType(TaskType.HTTP.name())
                      .parameters(new Object[] {})
                      .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                      .build())
            .build();

    PortalConfig portalConfig = new PortalConfig();
    portalConfig.setOptionalDelegateTaskRejectAtLimit(10000);
    when(mainConfiguration.getPortal()).thenReturn(portalConfig);

    delegateTaskServiceClassic.queueTask(delegateTask);
    assertThat(persistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.uuid, delegateTask.getUuid()).get())
        .isEqualTo(delegateTask);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  @Ignore("Ignore until we replace noop with real exception")
  public void shouldNotSaveDelegateTaskWhenRankLimitIsReached() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .uuid(generateUuid())
                                    .accountId(ACCOUNT_ID)
                                    .rank(DelegateTaskRank.IMPORTANT)
                                    .waitId(generateUuid())
                                    .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
                                    .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, ENV_ID)
                                    .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, INFRA_MAPPING_ID)
                                    .setupAbstraction(Cd1SetupFields.SERVICE_TEMPLATE_ID_FIELD, SERVICE_TEMPLATE_ID)
                                    .setupAbstraction(Cd1SetupFields.ARTIFACT_STREAM_ID_FIELD, ARTIFACT_STREAM_ID)
                                    .version(VERSION)
                                    .data(TaskData.builder()
                                              .async(true)
                                              .taskType(TaskType.HTTP.name())
                                              .parameters(new Object[] {})
                                              .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                              .build())
                                    .build();

    PortalConfig portalConfig = new PortalConfig();
    portalConfig.setImportantDelegateTaskRejectAtLimit(0);
    when(mainConfiguration.getPortal()).thenReturn(portalConfig);

    assertThatThrownBy(() -> delegateTaskServiceClassic.queueTask(delegateTask))
        .isInstanceOf(RateLimitExceededException.class)
        .hasMessage("Rate limit exceeded for task rank IMPORTANT. Please try again later.");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldProcessDelegateTaskResponse() {
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    delegateTaskService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build())
            .responseCode(ResponseCode.OK)
            .build());
    assertThat(persistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.uuid, delegateTask.getUuid()).get())
        .isEqualTo(null);
    verify(waitNotifyEngine)
        .doneWith(
            delegateTask.getWaitId(), ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build());
    verify(delegateTaskStatusObserverSubject).fireInform(any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldProcessDelegateTaskResponseWithoutWaitId() {
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    delegateTaskService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build())
            .build());
    assertThat(persistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.uuid, delegateTask.getUuid()).get())
        .isEqualTo(null);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldProcessSyncDelegateTaskResponse() {
    DelegateTask delegateTask = saveDelegateTask(false, emptySet(), QUEUED);
    delegateTaskService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build())
            .build());
    delegateTask = persistence.get(DelegateTask.class, delegateTask.getUuid());
    assertThat(delegateTask).isNull();
  }

  @Cache
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldAcquireTaskWhenQueued() {
    when(assignDelegateService.isWhitelisted(any(DelegateTask.class), any(String.class))).thenReturn(true);
    when(assignDelegateService.canAssign(
             any(BatchDelegateSelectionLog.class), any(String.class), any(DelegateTask.class)))
        .thenReturn(true);
    Delegate delegate = createDelegateBuilder().build();
    delegate.setUuid(DELEGATE_ID);
    persistence.save(delegate);
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    DelegateTaskPackage delegateTaskPackage =
        delegateTaskServiceClassic.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid());
    assertThat(delegateTaskPackage).isNotNull();
    assertThat(delegateTaskPackage.getDelegateTaskId()).isEqualTo(delegateTask.getUuid());
    assertThat(delegateTaskPackage.getDelegateId()).isEqualTo(DELEGATE_ID);
    assertThat(delegateTaskPackage.getAccountId()).isEqualTo(ACCOUNT_ID);
  }

  @Cache
  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldAcquireTaskWithoutValidation() {
    featureTestHelper.enableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
    when(assignDelegateService.canAssign(
             any(BatchDelegateSelectionLog.class), any(String.class), any(DelegateTask.class)))
        .thenReturn(true);
    Delegate delegate = createDelegateBuilder().build();
    delegate.setUuid(DELEGATE_ID);
    persistence.save(delegate);
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    DelegateTaskPackage delegateTaskPackage =
        delegateTaskServiceClassic.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid());
    assertThat(delegateTaskPackage).isNotNull();
    assertThat(delegateTaskPackage.getDelegateTaskId()).isEqualTo(delegateTask.getUuid());
    assertThat(delegateTaskPackage.getDelegateId()).isEqualTo(DELEGATE_ID);
    assertThat(delegateTaskPackage.getAccountId()).isEqualTo(ACCOUNT_ID);
    verify(assignDelegateService, never()).isWhitelisted(any(DelegateTask.class), any(String.class));
    featureTestHelper.disableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
  }

  @Cache
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldAcquireTaskWhenQueued_notWhitelisted() {
    when(assignDelegateService.isWhitelisted(any(DelegateTask.class), any(String.class))).thenReturn(false);
    when(assignDelegateService.shouldValidate(any(DelegateTask.class), any(String.class))).thenReturn(true);
    when(assignDelegateService.canAssign(
             any(BatchDelegateSelectionLog.class), any(String.class), any(DelegateTask.class)))
        .thenReturn(true);
    Delegate delegate = createDelegateBuilder().build();
    delegate.setUuid(DELEGATE_ID);
    persistence.save(delegate);
    delegateCache.get(ACCOUNT_ID, DELEGATE_ID, true);
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    assertThat(delegateTaskServiceClassic.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid()))
        .isNotNull();
  }

  @Cache
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldAcquireTaskWhenQueued_cannotAssign() {
    when(assignDelegateService.canAssign(
             any(BatchDelegateSelectionLog.class), any(String.class), any(DelegateTask.class)))
        .thenReturn(false);
    Delegate delegate = createDelegateBuilder().build();
    delegate.setUuid(DELEGATE_ID);
    persistence.save(delegate);
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    assertThat(delegateTaskServiceClassic.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid()))
        .isNull();
  }

  @Cache
  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void shouldNotAcquireTaskWhenNoEligibleDelegates_cannotAssign() {
    when(assignDelegateService.canAssign(
             any(BatchDelegateSelectionLog.class), any(String.class), any(DelegateTask.class)))
        .thenReturn(false);
    when(assignDelegateService.retrieveActiveDelegates(eq(ACCOUNT_ID), any(BatchDelegateSelectionLog.class)))
        .thenReturn(singletonList(DELEGATE_ID));
    Delegate delegate = createDelegateBuilder().build();
    delegate.setUuid(DELEGATE_ID);
    persistence.save(delegate);
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    assertThat(delegateTaskServiceClassic.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid()))
        .isNull();
  }

  @Cache
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldAcquireTaskWhenQueued_blacklisted() {
    when(assignDelegateService.isWhitelisted(any(DelegateTask.class), any(String.class))).thenReturn(false);
    when(assignDelegateService.shouldValidate(any(DelegateTask.class), any(String.class))).thenReturn(false);
    when(assignDelegateService.canAssign(
             any(BatchDelegateSelectionLog.class), any(String.class), any(DelegateTask.class)))
        .thenReturn(true);
    Delegate delegate = createDelegateBuilder().build();
    delegate.setUuid(DELEGATE_ID);
    persistence.save(delegate);
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    assertThat(delegateTaskServiceClassic.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid()))
        .isNull();
  }

  @Cache
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotAcquireTaskWhenAlreadyAcquired() {
    Delegate delegate = createDelegateBuilder().build();
    delegate.setUuid(DELEGATE_ID + "1");
    persistence.save(delegate);
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    assertThat(delegateTaskServiceClassic.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID + "1", delegateTask.getUuid()))
        .isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldNotAcquireTaskIfDelegateStatusNotEnabled() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(accountId);
    delegate.setUuid(delegateId);
    delegate.setStatus(DelegateInstanceStatus.WAITING_FOR_APPROVAL);
    persistence.save(delegate);

    DelegateTaskPackage delegateTaskPackage =
        delegateTaskServiceClassic.acquireDelegateTask(accountId, delegateId, generateUuid());
    assertThat(delegateTaskPackage).isNull();

    delegate.setStatus(DelegateInstanceStatus.DELETED);
    persistence.save(delegate);

    delegateTaskPackage = delegateTaskServiceClassic.acquireDelegateTask(accountId, delegateId, generateUuid());
    assertThat(delegateTaskPackage).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldNotAcquireTaskIfDelegateNotFoundInDb() {
    DelegateTaskPackage delegateTaskPackage =
        delegateTaskServiceClassic.acquireDelegateTask(ACCOUNT_ID, generateUuid(), generateUuid());
    assertThat(delegateTaskPackage).isNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFilterTaskForAccountOnAbort() {
    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(ACCOUNT_ID + "1");
    delegate.setUuid(DELEGATE_ID);
    persistence.save(delegate);
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    assertThat(delegateTaskServiceClassic.filter(DELEGATE_ID,
                   aDelegateTaskAbortEvent()
                       .withDelegateTaskId(delegateTask.getUuid())
                       .withAccountId(ACCOUNT_ID + "1")
                       .withSync(true)
                       .build()))
        .isFalse();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldReportConnectionResults_success() {
    DelegateTask delegateTask = saveDelegateTask(false, emptySet(), QUEUED);
    DelegateTaskPackage delegateTaskPackage =
        delegateTaskServiceClassic.reportConnectionResults(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
            singletonList(DelegateConnectionResult.builder()
                              .accountId(ACCOUNT_ID)
                              .delegateId(DELEGATE_ID)
                              .duration(100L)
                              .criteria("aaa")
                              .validated(true)
                              .build()));
    assertThat(delegateTaskPackage).isNotNull();
    assertThat(persistence.get(DelegateTask.class, delegateTask.getUuid()).getDelegateId()).isEqualTo(DELEGATE_ID);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldReportConnectionResults_fail() {
    DelegateTask delegateTask = saveDelegateTask(false, emptySet(), QUEUED);
    DelegateTaskPackage delegateTaskPackage =
        delegateTaskServiceClassic.reportConnectionResults(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
            singletonList(DelegateConnectionResult.builder()
                              .accountId(ACCOUNT_ID)
                              .delegateId(DELEGATE_ID)
                              .duration(100L)
                              .criteria("aaa")
                              .validated(false)
                              .build()));
    assertThat(delegateTaskPackage).isNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldReportConnectionResults_unavailable() {
    DelegateTask delegateTask = saveDelegateTask(false, emptySet(), STARTED);
    DelegateTaskPackage delegateTaskPackage =
        delegateTaskServiceClassic.reportConnectionResults(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
            singletonList(DelegateConnectionResult.builder()
                              .accountId(ACCOUNT_ID)
                              .delegateId(DELEGATE_ID)
                              .duration(100L)
                              .criteria("aaa")
                              .validated(true)
                              .build()));
    assertThat(delegateTaskPackage).isNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFailIfAllDelegatesFailed_all() {
    DelegateTask delegateTask = saveDelegateTask(true, ImmutableSet.of(DELEGATE_ID), QUEUED);
    when(assignDelegateService.connectedWhitelistedDelegates(delegateTask)).thenReturn(emptyList());
    delegateTaskServiceClassic.failIfAllDelegatesFailed(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(), true);
    verify(assignDelegateService).connectedWhitelistedDelegates(delegateTask);
    assertThat(persistence.createQuery(DelegateTask.class).get()).isNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFailIfAllDelegatesFailed_sync() {
    DelegateTask delegateTask = saveDelegateTask(false, ImmutableSet.of(DELEGATE_ID), QUEUED);
    when(assignDelegateService.connectedWhitelistedDelegates(delegateTask)).thenReturn(emptyList());
    delegateTaskServiceClassic.failIfAllDelegatesFailed(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(), true);
    verify(assignDelegateService).connectedWhitelistedDelegates(delegateTask);
    String expectedMessage =
        "No eligible delegates could perform the required capabilities for this task: [ https://www.google.com ]\n"
        + "  -  The capabilities were tested by the following delegates: [ DELEGATE_ID ]\n"
        + "  -  Following delegates were validating but never returned: [  ]\n"
        + "  -  Other delegates (if any) may have been offline or were not eligible due to tag or scope restrictions.";
    RemoteMethodReturnValueData notifyResponse = (RemoteMethodReturnValueData) kryoSerializer.asInflatedObject(
        persistence.createQuery(DelegateSyncTaskResponse.class).get().getResponseData());

    assertThat(notifyResponse.getException().getMessage()).isEqualTo(expectedMessage);
  }

  @Test
  @Owner(developers = MARKOM)
  @Category(UnitTests.class)
  public void shouldFailIfAllDelegatesFailedNoClientTools_sync() {
    DelegateTask delegateTask = saveDelegateTask(false, ImmutableSet.of(DELEGATE_ID), QUEUED);
    when(assignDelegateService.connectedWhitelistedDelegates(delegateTask)).thenReturn(emptyList());
    delegateTaskServiceClassic.failIfAllDelegatesFailed(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(), false);
    verify(assignDelegateService).connectedWhitelistedDelegates(delegateTask);
    String expectedMessage =
        "No eligible delegates could perform the required capabilities for this task: [ https://www.google.com ]\n"
        + "  -  The capabilities were tested by the following delegates: [ DELEGATE_ID ]\n"
        + "  -  Following delegates were validating but never returned: [  ]\n"
        + "  -  Other delegates (if any) may have been offline or were not eligible due to tag or scope restrictions."
        + "  -  This could be due to some client tools still being installed on the delegates. If this is the reason please retry in a few minutes.";
    RemoteMethodReturnValueData notifyResponse = (RemoteMethodReturnValueData) kryoSerializer.asInflatedObject(
        persistence.createQuery(DelegateSyncTaskResponse.class).get().getResponseData());

    assertThat(notifyResponse.getException().getMessage()).isEqualTo(expectedMessage);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFailIfAllDelegatesFailed_notAll() {
    DelegateTask delegateTask = saveDelegateTask(true, ImmutableSet.of(DELEGATE_ID, "delegate2"), QUEUED);
    delegateTaskServiceClassic.failIfAllDelegatesFailed(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(), true);
    assertThat(persistence.createQuery(DelegateTask.class).get()).isEqualTo(delegateTask);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFailIfAllDelegatesFailed_whitelist() {
    DelegateTask delegateTask = saveDelegateTask(true, ImmutableSet.of(DELEGATE_ID), QUEUED);
    when(assignDelegateService.connectedWhitelistedDelegates(delegateTask)).thenReturn(singletonList("delegate2"));
    delegateTaskServiceClassic.failIfAllDelegatesFailed(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(), true);
    verify(assignDelegateService).connectedWhitelistedDelegates(delegateTask);
    assertThat(persistence.createQuery(DelegateTask.class).get()).isEqualTo(delegateTask);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldExpireTask() {
    DelegateTask delegateTask = saveDelegateTask(true, ImmutableSet.of(DELEGATE_ID), QUEUED);
    delegateTaskServiceClassic.expireTask(ACCOUNT_ID, delegateTask.getUuid());
    assertThat(persistence.createQuery(DelegateTask.class).get().getStatus()).isEqualTo(ERROR);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldAbortTask() {
    DelegateTask delegateTask = saveDelegateTask(true, ImmutableSet.of(DELEGATE_ID), QUEUED);

    DelegateTask oldTask = delegateTaskServiceClassic.abortTask(ACCOUNT_ID, delegateTask.getUuid());

    assertThat(oldTask.getUuid()).isEqualTo(delegateTask.getUuid());
    assertThat(oldTask.getStatus()).isEqualTo(QUEUED);
    assertThat(persistence.createQuery(DelegateTask.class).get().getStatus()).isEqualTo(ABORTED);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldScheduleSyncTaskThrowNoInstalledDelegatesException() {
    thrown.expect(NoInstalledDelegatesException.class);
    when(assignDelegateService.retrieveActiveDelegates(anyString(), any())).thenReturn(emptyList());
    TaskData taskData = TaskData.builder().taskType(TaskType.HELM_COMMAND_TASK.name()).build();
    DelegateTask task = DelegateTask.builder().accountId(ACCOUNT_ID).delegateId(DELEGATE_ID).data(taskData).build();
    when(assignDelegateService.noInstalledDelegates(ACCOUNT_ID)).thenReturn(true);

    delegateTaskServiceClassic.scheduleSyncTask(task);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldScheduleSyncTaskThrowNoAvailableDelegatesException() {
    thrown.expect(NoAvailableDelegatesException.class);
    when(assignDelegateService.retrieveActiveDelegates(anyString(), any())).thenReturn(emptyList());
    TaskData taskData = TaskData.builder().taskType(TaskType.HELM_COMMAND_TASK.name()).build();
    DelegateTask task = DelegateTask.builder().accountId(ACCOUNT_ID).delegateId(DELEGATE_ID).data(taskData).build();
    when(assignDelegateService.noInstalledDelegates(ACCOUNT_ID)).thenReturn(false);

    delegateTaskServiceClassic.scheduleSyncTask(task);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testConvertSelectorsToExecutionCapabilityTaskSelectors_TaskSelectors() {
    List<String> selectors = Arrays.asList("a", "b");

    DelegateTask delegateTask =
        DelegateTask.builder().accountId(ACCOUNT_ID).delegateId(DELEGATE_ID).tags(selectors).build();

    delegateTaskServiceClassic.convertToExecutionCapability(delegateTask);

    List<SelectorCapability> selectorsCapabilityList = delegateTask.getExecutionCapabilities()
                                                           .stream()
                                                           .filter(c -> c instanceof SelectorCapability)
                                                           .map(c -> (SelectorCapability) c)
                                                           .collect(Collectors.toList());

    assertThat(selectorsCapabilityList.get(0).getSelectorOrigin()).isEqualTo(TASK_SELECTORS);
    assertThat(selectorsCapabilityList.get(0).getSelectors()).isEqualTo(new HashSet<>(selectors));
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testConvertSelectorsToExecutionCapabilityTaskSelectors_TaskCategory() {
    Set<String> commandMapSelectors = new HashSet<>();
    commandMapSelectors.add("eee");
    TaskSelectorMap sampleMap = TaskSelectorMap.builder()
                                    .accountId(ACCOUNT_ID)
                                    .taskGroup(TaskGroup.HTTP)
                                    .selectors(commandMapSelectors)
                                    .build();
    persistence.save(sampleMap);

    // CG Task
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .delegateId(DELEGATE_ID)
                                    .data(TaskData.builder().taskType(TaskType.HTTP.name()).build())
                                    .build();

    delegateTaskServiceClassic.convertToExecutionCapability(delegateTask);

    List<SelectorCapability> selectorsCapabilityList = delegateTask.getExecutionCapabilities()
                                                           .stream()
                                                           .filter(c -> c instanceof SelectorCapability)
                                                           .map(c -> (SelectorCapability) c)
                                                           .collect(Collectors.toList());

    assertThat(selectorsCapabilityList.get(0).getSelectorOrigin()).isEqualTo(TASK_CATEGORY_MAP);
    assertThat(selectorsCapabilityList.get(0).getSelectors()).isEqualTo(sampleMap.getSelectors());

    // NG Task
    DelegateTask delegateTaskNg = DelegateTask.builder()
                                      .accountId(ACCOUNT_ID)
                                      .delegateId(DELEGATE_ID)
                                      .data(TaskData.builder().taskType(TaskType.HTTP.name()).build())
                                      .setupAbstraction("ng", "true")
                                      .build();

    delegateTaskServiceClassic.convertToExecutionCapability(delegateTaskNg);

    selectorsCapabilityList = delegateTaskNg.getExecutionCapabilities()
                                  .stream()
                                  .filter(c -> c instanceof SelectorCapability)
                                  .map(c -> (SelectorCapability) c)
                                  .collect(Collectors.toList());

    assertThat(selectorsCapabilityList).isEmpty();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testBuildCapabilitiesCheckTask() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    CapabilityCheckDetails capabilityCheckDetails1 = buildCapabilityCheckDetails(accountId, delegateId, generateUuid());
    CapabilityCheckDetails capabilityCheckDetails2 = buildCapabilityCheckDetails(accountId, delegateId, generateUuid());

    DelegateTask capabilitiesCheckTask = delegateTaskServiceClassic.buildCapabilitiesCheckTask(
        accountId, delegateId, asList(capabilityCheckDetails1, capabilityCheckDetails2));

    assertThat(capabilitiesCheckTask).isNotNull();
    assertThat(capabilitiesCheckTask.getAccountId()).isEqualTo(accountId);
    assertThat(capabilitiesCheckTask.getRank()).isEqualTo(DelegateTaskRank.CRITICAL);
    assertThat(capabilitiesCheckTask.getMustExecuteOnDelegateId()).isEqualTo(delegateId);
    assertThat(capabilitiesCheckTask.getData()).isNotNull();
    assertThat(capabilitiesCheckTask.getData().isAsync()).isFalse();
    assertThat(capabilitiesCheckTask.getData().getTaskType()).isEqualTo(TaskType.BATCH_CAPABILITY_CHECK.name());
    assertThat(capabilitiesCheckTask.getData().getTimeout()).isEqualTo(TimeUnit.MINUTES.toMillis(1L));
    assertThat(capabilitiesCheckTask.getData().getParameters()).hasSize(1);
    assertThat(capabilitiesCheckTask.getData().getParameters()[0])
        .isInstanceOf(BatchCapabilityCheckTaskParameters.class);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testCreateCapabilityTaskSelectionDetailsInstance() {
    DelegateTask task = DelegateTask.builder().build();
    CapabilityRequirement capabilityRequirement = buildCapabilityRequirement();

    // Test case with partial arguments
    delegateTaskServiceClassic.createCapabilityTaskSelectionDetailsInstance(task, capabilityRequirement, null);
    verify(capabilityService)
        .buildCapabilityTaskSelectionDetails(capabilityRequirement, null, task.getSetupAbstractions(), null, null);

    // Test case with all arguments
    task.setData(TaskData.builder().taskType(TaskType.HTTP.name()).build());

    Map<String, String> taskSetupAbstractions = new HashMap<>();
    taskSetupAbstractions.put("foo", "bar");
    task.setSetupAbstractions(taskSetupAbstractions);

    task.setExecutionCapabilities(Arrays.asList(
        SelectorCapability.builder().selectorOrigin("TASK_SELECTORS").selectors(Collections.singleton("sel1")).build(),
        HttpConnectionExecutionCapability.builder().url("https://google.com").build()));

    List<String> assignableDelegateIds = Arrays.asList("del1", "del2");

    delegateTaskServiceClassic.createCapabilityTaskSelectionDetailsInstance(
        task, capabilityRequirement, assignableDelegateIds);

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(capabilityService)
        .buildCapabilityTaskSelectionDetails(eq(capabilityRequirement), eq(TaskGroup.HTTP),
            eq(task.getSetupAbstractions()), captor.capture(), eq(assignableDelegateIds));
    List<SelectorCapability> selectorCapabilities = captor.getValue();
    assertThat(selectorCapabilities).hasSize(1);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testExecuteBatchCapabilityCheckTaskWithNotInScopeDelegate() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    String capabilityId = generateUuid();

    CapabilitySubjectPermission permission1 =
        buildCapabilitySubjectPermission(accountId, delegateId, capabilityId, PermissionResult.ALLOWED);

    CapabilityTaskSelectionDetails taskSelectionDetails = buildCapabilityTaskSelectionDetails();
    when(capabilityService.getAllCapabilityTaskSelectionDetails(accountId, capabilityId))
        .thenReturn(Collections.singletonList(taskSelectionDetails));
    when(assignDelegateService.canAssign(any(null), anyString(), eq(accountId), eq("app1"), eq("env1"), eq("infra1"),
             eq(taskSelectionDetails.getTaskGroup()), any(List.class),
             eq(taskSelectionDetails.getTaskSetupAbstractions())))
        .thenReturn(false);
    when(capabilityService.getNotDeniedCapabilityPermissions(accountId, capabilityId))
        .thenReturn(Collections.singletonList(
            buildCapabilitySubjectPermission(accountId, generateUuid(), capabilityId, PermissionResult.ALLOWED)));

    delegateTaskServiceClassic.executeBatchCapabilityCheckTask(
        accountId, delegateId, Collections.singletonList(permission1), null);
    verify(capabilityService).deleteCapabilitySubjectPermission(permission1.getUuid());
    verify(delegateSyncService, never()).waitForTask(any(), any(), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testExecuteBatchCapabilityCheckTaskWithNoCapabilityFound() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    String capabilityId = generateUuid();
    String blockedTaskSelectionDetailsId = generateUuid();

    CapabilitySubjectPermission permission1 =
        buildCapabilitySubjectPermission(accountId, delegateId, capabilityId, PermissionResult.ALLOWED);

    delegateTaskServiceClassic.executeBatchCapabilityCheckTask(
        accountId, delegateId, Collections.singletonList(permission1), blockedTaskSelectionDetailsId);
    verify(capabilityService, never()).deleteCapabilitySubjectPermission(any());
    verify(delegateSyncService, never()).waitForTask(any(), any(), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testExecuteBatchCapabilityCheckTaskWithNullTaskResponse() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    String capabilityId = generateUuid();
    String blockedTaskSelectionDetailsId = generateUuid();

    CapabilitySubjectPermission permission1 =
        buildCapabilitySubjectPermission(accountId, delegateId, capabilityId, PermissionResult.ALLOWED);

    CapabilityRequirement capabilityRequirement = buildCapabilityRequirement();
    capabilityRequirement.setAccountId(accountId);
    capabilityRequirement.setUuid(capabilityId);
    persistence.save(capabilityRequirement);

    when(assignDelegateService.retrieveActiveDelegates(any(), any())).thenReturn(Collections.singletonList(delegateId));
    when(assignDelegateService.canAssign(any(), any(), any())).thenReturn(true);
    when(delegateSyncService.waitForTask(any(), any(), any())).thenReturn(null);

    delegateTaskServiceClassic.executeBatchCapabilityCheckTask(
        accountId, delegateId, Collections.singletonList(permission1), blockedTaskSelectionDetailsId);
    verify(capabilityService, never()).deleteCapabilitySubjectPermission(any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testExecuteBatchCapabilityCheckTaskWithErrorTaskResponse() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    String capabilityId = generateUuid();
    String blockedTaskSelectionDetailsId = generateUuid();

    CapabilitySubjectPermission permission1 =
        buildCapabilitySubjectPermission(accountId, delegateId, capabilityId, PermissionResult.ALLOWED);

    CapabilityRequirement capabilityRequirement = buildCapabilityRequirement();
    capabilityRequirement.setAccountId(accountId);
    capabilityRequirement.setUuid(capabilityId);
    persistence.save(capabilityRequirement);

    when(assignDelegateService.retrieveActiveDelegates(any(), any())).thenReturn(Collections.singletonList(delegateId));
    when(assignDelegateService.canAssign(any(), any(), any())).thenReturn(true);
    when(delegateSyncService.waitForTask(any(), any(), any()))
        .thenReturn(RemoteMethodReturnValueData.builder().exception(new InvalidRequestException("")).build());

    delegateTaskServiceClassic.executeBatchCapabilityCheckTask(
        accountId, delegateId, Collections.singletonList(permission1), blockedTaskSelectionDetailsId);
    verify(capabilityService, never()).deleteCapabilitySubjectPermission(any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testExecuteBatchCapabilityCheckTaskWithSuccessfulTaskResponse() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    String capabilityId = generateUuid();

    CapabilitySubjectPermission permission1 =
        buildCapabilitySubjectPermission(accountId, delegateId, capabilityId, PermissionResult.UNCHECKED);
    persistence.save(permission1);

    CapabilityRequirement capabilityRequirement = buildCapabilityRequirement();
    capabilityRequirement.setAccountId(accountId);
    capabilityRequirement.setUuid(capabilityId);
    persistence.save(capabilityRequirement);

    String blockedTaskSelectionDetailsId = generateUuid();
    CapabilityTaskSelectionDetails taskSelectionDetails = buildCapabilityTaskSelectionDetails();
    taskSelectionDetails.setUuid(blockedTaskSelectionDetailsId);
    taskSelectionDetails.setAccountId(accountId);
    taskSelectionDetails.setCapabilityId(capabilityId);
    persistence.save(taskSelectionDetails);

    CapabilityCheckDetails capabilityCheckDetails = buildCapabilityCheckDetails(accountId, delegateId, capabilityId)
                                                        .toBuilder()
                                                        .permissionResult(PermissionResult.ALLOWED)
                                                        .build();

    when(assignDelegateService.retrieveActiveDelegates(any(), any())).thenReturn(Collections.singletonList(delegateId));
    when(assignDelegateService.canAssign(any(), any(), any())).thenReturn(true);
    when(delegateSyncService.waitForTask(any(), any(), any()))
        .thenReturn(BatchCapabilityCheckTaskResponse.builder()
                        .capabilityCheckDetailsList(Collections.singletonList(capabilityCheckDetails))
                        .build());

    delegateTaskServiceClassic.executeBatchCapabilityCheckTask(
        accountId, delegateId, Collections.singletonList(permission1), blockedTaskSelectionDetailsId);
    verify(capabilityService, never()).deleteCapabilitySubjectPermission(any());

    CapabilitySubjectPermission updatedPermission =
        persistence.get(CapabilitySubjectPermission.class, permission1.getUuid());
    assertThat(updatedPermission.getPermissionResult()).isEqualTo(PermissionResult.ALLOWED);
    assertThat(updatedPermission.getRevalidateAfter()).isGreaterThan(System.currentTimeMillis());
    assertThat(updatedPermission.getMaxValidUntil()).isGreaterThan(System.currentTimeMillis());

    CapabilityTaskSelectionDetails updatedTaskSelectionDetails =
        persistence.get(CapabilityTaskSelectionDetails.class, taskSelectionDetails.getUuid());
    assertThat(updatedTaskSelectionDetails.isBlocked()).isFalse();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testUpsertCapabilityRequirements() {
    String accountId = generateUuid();
    // Test FF disabled
    delegateTaskServiceClassic.upsertCapabilityRequirements(DelegateTask.builder().accountId(accountId).build());

    // Test FF enabled, but no task capabilities
    featureTestHelper.enableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
    delegateTaskServiceClassic.upsertCapabilityRequirements(DelegateTask.builder().accountId(accountId).build());

    // Test FF enabled, but no task AGENT capabilities
    delegateTaskServiceClassic.upsertCapabilityRequirements(
        DelegateTask.builder()
            .accountId(accountId)
            .executionCapabilities(Collections.singletonList(SelectorCapability.builder()
                                                                 .selectorOrigin("TASK_SELECTORS")
                                                                 .selectors(Collections.singleton("sel1"))
                                                                 .build()))
            .build());

    verify(assignDelegateService, never()).getAccountDelegates(any());

    // Test full scenario
    DelegateTask task = DelegateTask.builder()
                            .accountId(accountId)
                            .executionCapabilities(asList(SelectorCapability.builder()
                                                              .selectorOrigin("TASK_SELECTORS")
                                                              .selectors(Collections.singleton("sel1"))
                                                              .build(),
                                HttpConnectionExecutionCapability.builder().url("https://google.com").build()))
                            .build();
    Delegate delegate =
        Delegate.builder().uuid(generateUuid()).accountId(accountId).status(DelegateInstanceStatus.ENABLED).build();
    when(assignDelegateService.getAccountDelegates(accountId)).thenReturn(Collections.singletonList(delegate));
    when(assignDelegateService.canAssign(null, delegate.getUuid(), task)).thenReturn(true);
    BatchDelegateSelectionLog selectionLogBatch = BatchDelegateSelectionLog.builder().build();
    when(delegateSelectionLogsService.createBatch(task)).thenReturn(selectionLogBatch);
    when(capabilityService.buildCapabilityRequirement(any(), any())).thenReturn(buildCapabilityRequirement());
    when(capabilityService.buildCapabilityTaskSelectionDetails(any(), any(), any(), any(), any()))
        .thenReturn(buildCapabilityTaskSelectionDetails());

    delegateTaskServiceClassic.upsertCapabilityRequirements(task);

    verify(delegateSelectionLogsService).createBatch(task);
    verify(delegateSelectionLogsService).save(selectionLogBatch);
    verify(capabilityService)
        .processTaskCapabilityRequirement(
            any(CapabilityRequirement.class), any(CapabilityTaskSelectionDetails.class), any(List.class));

    featureTestHelper.disableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testPickDelegateForTaskWithoutAnyAgentCapabilities() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    DelegateTask task = DelegateTask.builder().build();

    // Test no active delegates case
    assertThat(delegateTaskServiceClassic.pickDelegateForTaskWithoutAnyAgentCapabilities(task, null)).isNull();

    // Test assignable delegate with ignore already tried case
    BatchDelegateSelectionLog selectionLogBatch = BatchDelegateSelectionLog.builder().build();
    when(delegateSelectionLogsService.createBatch(task)).thenReturn(selectionLogBatch);
    when(assignDelegateService.canAssign(eq(selectionLogBatch), anyString(), eq(task))).thenReturn(true);

    assertThat(delegateTaskServiceClassic.pickDelegateForTaskWithoutAnyAgentCapabilities(
                   task, Collections.singletonList(delegateId)))
        .isEqualTo(delegateId);
    verify(delegateSelectionLogsService).createBatch(task);
    verify(delegateSelectionLogsService).save(selectionLogBatch);

    task.setAlreadyTriedDelegates(Stream.of(delegateId).collect(Collectors.toSet()));
    assertThat(delegateTaskServiceClassic.pickDelegateForTaskWithoutAnyAgentCapabilities(
                   task, Collections.singletonList(delegateId)))
        .isEqualTo(delegateId);
    verify(delegateSelectionLogsService, times(2)).createBatch(task);
    verify(delegateSelectionLogsService, times(2)).save(selectionLogBatch);

    // Test assignable delegate without ignoring already tried case
    String delegateId2 = generateUuid();
    assertThat(delegateTaskServiceClassic.pickDelegateForTaskWithoutAnyAgentCapabilities(
                   task, Arrays.asList(delegateId, delegateId2)))
        .isEqualTo(delegateId2);
    verify(delegateSelectionLogsService, times(3)).createBatch(task);
    verify(delegateSelectionLogsService, times(3)).save(selectionLogBatch);

    // Test out of scope case
    when(assignDelegateService.canAssign(eq(selectionLogBatch), anyString(), eq(task))).thenReturn(false);
    assertThat(delegateTaskServiceClassic.pickDelegateForTaskWithoutAnyAgentCapabilities(
                   task, Arrays.asList(delegateId, delegateId2)))
        .isNull();
    verify(delegateSelectionLogsService, times(4)).createBatch(task);
    verify(delegateSelectionLogsService, times(4)).save(selectionLogBatch);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testObtainCapableDelegateId() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    String delegateId2 = generateUuid();
    DelegateTask task = DelegateTask.builder().uuid(generateUuid()).accountId(accountId).build();

    // Old way with delegate whitelisting
    when(assignDelegateService.pickFirstAttemptDelegate(task)).thenReturn(delegateId);
    assertThat(delegateTaskServiceClassic.obtainCapableDelegateId(task, null)).isEqualTo(delegateId);
    verify(delegateSelectionLogsService, never()).createBatch(task);

    featureTestHelper.enableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
    BatchDelegateSelectionLog selectionLogBatch = BatchDelegateSelectionLog.builder().build();
    when(delegateSelectionLogsService.createBatch(task)).thenReturn(selectionLogBatch);
    List<String> activeDelegates = Arrays.asList(delegateId, delegateId2);

    // Test no task capabilities case
    when(assignDelegateService.retrieveActiveDelegates(task.getAccountId(), selectionLogBatch)).thenReturn(null);
    assertThat(delegateTaskServiceClassic.obtainCapableDelegateId(task, null)).isNull();
    verify(delegateSelectionLogsService).createBatch(task);
    verify(delegateSelectionLogsService).save(selectionLogBatch);

    // Test no AGENT capabilities case
    task.setExecutionCapabilities(asList(SelectorCapability.builder()
                                             .selectorOrigin("TASK_SELECTORS")
                                             .selectors(Collections.singleton("sel1"))
                                             .build()));
    // HttpConnectionExecutionCapability.builder().url("https://google.com").build()))
    assertThat(delegateTaskServiceClassic.obtainCapableDelegateId(task, null)).isNull();

    // Test assignable delegate with ignoring already tried case
    task.setExecutionCapabilities(asList(
        SelectorCapability.builder().selectorOrigin("TASK_SELECTORS").selectors(Collections.singleton("sel1")).build(),
        HttpConnectionExecutionCapability.builder().url("https://google.com").build()));
    when(assignDelegateService.retrieveActiveDelegates(task.getAccountId(), selectionLogBatch))
        .thenReturn(Arrays.asList(delegateId, delegateId2));
    when(capabilityService.buildCapabilityRequirement(any(), any())).thenReturn(buildCapabilityRequirement());

    String delegateId3 = generateUuid();
    when(capabilityService.getCapableDelegateIds(eq(task.getAccountId()), any()))
        .thenReturn(Collections.emptySet())
        .thenReturn(Stream.of(delegateId2, delegateId, delegateId3).collect(Collectors.toSet()));
    CapabilityTaskSelectionDetails taskSelectionDetails = buildCapabilityTaskSelectionDetails();
    when(capabilityService.getAllCapabilityTaskSelectionDetails(eq(accountId), anyString()))
        .thenReturn(Collections.singletonList(taskSelectionDetails));
    when(assignDelegateService.canAssign(any(null), eq(delegateId), eq(accountId), eq("app1"), eq("env1"), eq("infra1"),
             eq(taskSelectionDetails.getTaskGroup()), any(List.class),
             eq(taskSelectionDetails.getTaskSetupAbstractions())))
        .thenReturn(true);
    when(assignDelegateService.canAssign(any(null), eq(delegateId2), eq(accountId), eq("app1"), eq("env1"),
             eq("infra1"), eq(taskSelectionDetails.getTaskGroup()), any(List.class),
             eq(taskSelectionDetails.getTaskSetupAbstractions())))
        .thenReturn(false);
    when(capabilityService.getNotDeniedCapabilityPermissions(eq(accountId), anyString()))
        .thenReturn(Collections.singletonList(
            buildCapabilitySubjectPermission(accountId, delegateId, generateUuid(), PermissionResult.ALLOWED)));

    assertThat(delegateTaskServiceClassic.obtainCapableDelegateId(task, Collections.singleton(delegateId3)))
        .isEqualTo(delegateId);

    // Test case with no capable delegates
    when(capabilityService.getCapableDelegateIds(eq(task.getAccountId()), any())).thenReturn(Collections.emptySet());
    assertThat(delegateTaskServiceClassic.obtainCapableDelegateId(task, Collections.singleton(delegateId3))).isNull();

    featureTestHelper.disableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
  }

  private CapabilityCheckDetails buildCapabilityCheckDetails(String accountId, String delegateId, String capabilityId) {
    return CapabilityCheckDetails.builder()
        .accountId(accountId)
        .delegateId(delegateId)
        .capabilityId(capabilityId)
        .capabilityType(CapabilityType.HTTP)
        .capabilityParameters(
            CapabilityParameters.newBuilder()
                .setHttpConnectionParameters(HttpConnectionParameters.newBuilder().setUrl("https://google.com"))
                .build())
        .build();
  }

  private CapabilityRequirement buildCapabilityRequirement() {
    return CapabilityRequirement.builder()
        .accountId(generateUuid())
        .uuid(generateUuid())
        .validUntil(Date.from(Instant.now().plus(Duration.ofDays(30))))
        .capabilityType(CapabilityType.HTTP.name())
        .capabilityParameters(
            CapabilityParameters.newBuilder()
                .setHttpConnectionParameters(HttpConnectionParameters.newBuilder().setUrl("https://google.com"))
                .build())
        .build();
  }

  private CapabilityTaskSelectionDetails buildCapabilityTaskSelectionDetails() {
    Map<String, Set<String>> taskSelectors = new HashMap<>();
    taskSelectors.put("TASK_SELECTORS", Collections.singleton("value1"));
    taskSelectors.put("TASK_CATEGORY_MAP", Collections.singleton("value2"));

    Map<String, String> taskSetupAbstractions = new HashMap<>();
    taskSetupAbstractions.put("appId", "app1");
    taskSetupAbstractions.put("envId", "env1");
    taskSetupAbstractions.put("infrastructureMappingId", "infra1");
    taskSetupAbstractions.put("foo", "bar");

    return CapabilityTaskSelectionDetails.builder()
        .uuid(generateUuid())
        .accountId(generateUuid())
        .capabilityId(generateUuid())
        .taskGroup(TaskGroup.HTTP)
        .taskSelectors(taskSelectors)
        .taskSetupAbstractions(taskSetupAbstractions)
        .blocked(true)
        .validUntil(Date.from(Instant.now().plus(Duration.ofDays(30))))
        .build();
  }

  private CapabilitySubjectPermission buildCapabilitySubjectPermission(
      String accountId, String delegateId, String capabilityId, PermissionResult permissionResult) {
    return CapabilitySubjectPermission.builder()
        .uuid(generateUuid())
        .accountId(accountId)
        .delegateId(delegateId)
        .capabilityId(capabilityId)
        .maxValidUntil(120000)
        .revalidateAfter(180000)
        .validUntil(Date.from(Instant.now().plus(Duration.ofDays(30))))
        .permissionResult(permissionResult)
        .build();
  }

  private DelegateTask saveDelegateTask(boolean async, Set<String> validatingTaskIds, DelegateTask.Status status) {
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(ACCOUNT_ID)
            .waitId(generateUuid())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
            .version(VERSION)
            .data(TaskData.builder()
                      .async(async)
                      .taskType(TaskType.HTTP.name())
                      .parameters(new Object[] {HttpTaskParameters.builder().url("https://www.google.com").build()})
                      .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                      .build())
            .validatingDelegateIds(validatingTaskIds)
            .validationCompleteDelegateIds(ImmutableSet.of(DELEGATE_ID))
            .build();

    delegateTaskServiceClassic.saveDelegateTask(delegateTask, status);
    return delegateTask;
  }

  private DelegateBuilder createDelegateBuilder() {
    return Delegate.builder()
        .accountId(ACCOUNT_ID)
        .ip("127.0.0.1")
        .hostName("localhost")
        .delegateName("testDelegateName")
        .delegateType(DELEGATE_TYPE)
        .version(VERSION)
        .status(DelegateInstanceStatus.ENABLED)
        .lastHeartBeat(System.currentTimeMillis());
  }

  private static JreConfig getOpenjdkJreConfig() {
    return JreConfig.builder()
        .version("1.8.0_242")
        .jreDirectory("jdk8u242-b08-jre")
        .jreMacDirectory("jdk8u242-b08-jre")
        .jreTarPath("jre/openjdk-8u242/jre_x64_${OS}_8u242b08.tar.gz")
        .build();
  }
}
