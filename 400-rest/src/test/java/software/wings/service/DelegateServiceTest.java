package software.wings.service;

import static io.harness.beans.DelegateTask.Status.ABORTED;
import static io.harness.beans.DelegateTask.Status.ERROR;
import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.beans.DelegateTask.Status.STARTED;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.UUIDGenerator.generateTimeBasedUuid;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.DelegateProfile.DelegateProfileBuilder;
import static io.harness.delegate.beans.DelegateProfile.builder;
import static io.harness.delegate.beans.DelegateTaskAbortEvent.Builder.aDelegateTaskAbortEvent;
import static io.harness.delegate.beans.DelegateType.ECS;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.delegate.message.ManagerMessageConstants.SELF_DESTRUCT;
import static io.harness.obfuscate.Obfuscator.obfuscate;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.ANKIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.DESCRIPTION;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.MEHUL;
import static io.harness.rule.OwnerRule.NIKOLA;
import static io.harness.rule.OwnerRule.PUNEET;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.VUK;

import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.service.impl.DelegateServiceImpl.DELEGATE_DIR;
import static software.wings.service.impl.DelegateServiceImpl.DOCKER_DELEGATE;
import static software.wings.service.impl.DelegateServiceImpl.KUBERNETES_DELEGATE;
import static software.wings.service.impl.DelegateServiceImpl.TASK_CATEGORY_MAP;
import static software.wings.service.impl.DelegateServiceImpl.TASK_SELECTORS;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_GROUP_NAME;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_TEMPLATE_ID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.base.Charsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import io.harness.beans.EncryptedData;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.FeatureName;
import io.harness.beans.SearchFilter.Operator;
import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.CapabilityTaskSelectionDetails;
import io.harness.capability.HttpConnectionParameters;
import io.harness.capability.service.CapabilityService;
import io.harness.category.element.UnitTests;
import io.harness.configuration.DeployMode;
import io.harness.delegate.beans.ConnectionMode;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateBuilder;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.delegate.beans.DelegateApproval;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.delegate.beans.DelegateConnectionHeartbeat;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateParams;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfileParams;
import io.harness.delegate.beans.DelegateRegisterResponse;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateSetupDetails;
import io.harness.delegate.beans.DelegateSize;
import io.harness.delegate.beans.DelegateSizeDetails;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskEvent;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskRank;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.DelegateTaskResponse.ResponseCode;
import io.harness.delegate.beans.DuplicateDelegateException;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.FileMetadata;
import io.harness.delegate.beans.K8sConfigDetails;
import io.harness.delegate.beans.K8sPermissionType;
import io.harness.delegate.beans.NoAvailableDelegatesException;
import io.harness.delegate.beans.NoInstalledDelegatesException;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.TaskSelectorMap;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.executioncapability.BatchCapabilityCheckTaskParameters;
import io.harness.delegate.task.executioncapability.BatchCapabilityCheckTaskResponse;
import io.harness.delegate.task.executioncapability.CapabilityCheckDetails;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logstreaming.LogStreamingServiceConfig;
import io.harness.network.LocalhostUtils;
import io.harness.observer.Subject;
import io.harness.persistence.HPersistence;
import io.harness.rule.Cache;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.serializer.KryoSerializer;
import io.harness.service.dto.RetryDelegate;
import io.harness.service.intfc.DelegateCache;
import io.harness.service.intfc.DelegateInsightsService;
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
import software.wings.app.FileUploadLimit;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateScalingGroup;
import software.wings.beans.DelegateStatus;
import software.wings.beans.Event.Type;
import software.wings.beans.LicenseInfo;
import software.wings.beans.ServiceVariable;
import software.wings.beans.TaskType;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DelegateProfileErrorAlert;
import software.wings.cdn.CdnConfig;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.delegatetasks.cv.RateLimitExceededException;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.features.api.UsageLimitedFeature;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.jre.JreConfig;
import software.wings.licensing.LicenseService;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.DelegateConnectionDao;
import software.wings.service.impl.DelegateObserver;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.DelegateTaskBroadcastHelper;
import software.wings.service.impl.DelegateTaskStatusObserver;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.impl.infra.InfraDownloadService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionStatusData;
import software.wings.sm.states.JenkinsState.JenkinsExecutionResponse;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.google.inject.Inject;
import freemarker.template.TemplateException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import javax.ws.rs.core.MediaType;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@BreakDependencyOn("software.wings.WingsBaseTest")
public class DelegateServiceTest extends WingsBaseTest {
  private static final String VERSION = "1.0.0";
  private static final String DELEGATE_NAME = "harness-delegate";
  private static final String DELEGATE_PROFILE_ID = "QFWin33JRlKWKBzpzE5A9A";
  private static final String DELEGATE_TYPE = "dockerType";
  private static final String PRIMARY_PROFILE_NAME = "primary";

  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private AccountService accountService;
  @Mock private LicenseService licenseService;
  @Mock private EventEmitter eventEmitter;
  @Mock private MainConfiguration mainConfiguration;
  @Mock private BroadcasterFactory broadcasterFactory;
  @Mock private Broadcaster broadcaster;
  @Mock private AssignDelegateService assignDelegateService;
  @Mock private DelegateProfileService delegateProfileService;
  @Mock private InfraDownloadService infraDownloadService;
  @Mock private SecretManager secretManager;
  @Mock private ManagerDecryptionService managerDecryptionService;
  @Mock private FileService fileService;
  @Mock private AlertService alertService;
  @Mock private VersionInfoManager versionInfoManager;
  @Mock private SubdomainUrlHelperIntfc subdomainUrlHelper;
  @Mock private ConfigurationController configurationController;
  @Mock private AuditServiceHelper auditServiceHelper;
  @Mock private DelegateGrpcConfig delegateGrpcConfig;
  @Mock private CapabilityService capabilityService;
  @Mock private DelegateSyncService delegateSyncService;
  @Mock private DelegateSelectionLogsService delegateSelectionLogsService;
  @Mock private DelegateInsightsService delegateInsightsService;

  @Inject private FeatureTestHelper featureTestHelper;
  @Inject private DelegateConnectionDao delegateConnectionDao;
  @Inject private KryoSerializer kryoSerializer;

  private int port = LocalhostUtils.findFreePort();
  @Rule public WireMockRule wireMockRule = new WireMockRule(port);
  @Rule public ExpectedException thrown = ExpectedException.none();

  @InjectMocks @Inject private DelegateCache delegateCache;
  @InjectMocks @Inject private DelegateTaskBroadcastHelper delegateTaskBroadcastHelper;
  @InjectMocks @Inject private DelegateServiceImpl delegateService;
  @InjectMocks @Inject private DelegateTaskService delegateTaskService;

  @Mock private UsageLimitedFeature delegatesFeature;

  @Inject private HPersistence persistence;

  private final Subject<DelegateProfileObserver> delegateProfileSubject = mock(Subject.class);
  private final Subject<DelegateTaskRetryObserver> retryObserverSubject = mock(Subject.class);
  private final Subject<DelegateTaskStatusObserver> delegateTaskStatusObserverSubject = mock(Subject.class);
  private final Subject<DelegateObserver> subject = mock(Subject.class);

  private final Account account =
      anAccount().withLicenseInfo(LicenseInfo.builder().accountStatus(AccountStatus.ACTIVE).build()).build();

  private DelegateProfileBuilder createDelegateProfileBuilder() {
    return DelegateProfile.builder().name("DELEGATE_PROFILE_NAME").description("DELEGATE_PROFILE_DESC");
  }

  @Before
  public void setUp() throws IllegalAccessException {
    CdnConfig cdnConfig = new CdnConfig();
    cdnConfig.setUrl("http://localhost:9500");
    when(subdomainUrlHelper.getDelegateMetadataUrl(any(), any(), any()))
        .thenReturn("http://localhost:" + port + "/delegateci.txt");
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES);
    when(mainConfiguration.getKubectlVersion()).thenReturn("v1.12.2");
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
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldList() {
    String accountId = generateUuid();
    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(accountId);
    persistence.save(delegate);
    assertThat(delegateService.list(aPageRequest().addFilter(DelegateKeys.accountId, Operator.EQ, accountId).build()))
        .hasSize(1)
        .containsExactly(delegate);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGet() {
    String accountId = generateUuid();
    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(accountId);
    persistence.save(delegate);
    assertThat(delegateCache.get(accountId, delegate.getUuid(), true)).isEqualTo(delegate);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void shouldGetDelegateStatus() {
    String accountId = generateUuid();
    when(accountService.getDelegateConfiguration(anyString()))
        .thenReturn(DelegateConfiguration.builder().delegateVersions(asList(VERSION)).build());
    Delegate delegate = createDelegateBuilder().build();

    delegate.setAccountId(accountId);

    Delegate deletedDelegate = createDelegateBuilder().build();
    deletedDelegate.setAccountId(accountId);
    deletedDelegate.setStatus(DelegateInstanceStatus.DELETED);

    persistence.save(Arrays.asList(delegate, deletedDelegate));

    delegateService.registerHeartbeat(accountId, delegate.getUuid(),
        DelegateConnectionHeartbeat.builder().delegateConnectionId(generateUuid()).version(VERSION).build(),
        ConnectionMode.POLLING);
    DelegateStatus delegateStatus = delegateService.getDelegateStatus(accountId);
    assertThat(delegateStatus.getPublishedVersions()).hasSize(1).contains(VERSION);
    assertThat(delegateStatus.getDelegates()).hasSize(1);
    assertThat(delegateStatus.getDelegates().get(0).isActivelyConnected()).isTrue();

    validateDelegateInnerProperties(delegate.getUuid(), delegateStatus.getDelegates().get(0));
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldGetDelegateStatus2WithoutScalingGroups() {
    String accountId = generateUuid();
    when(accountService.getDelegateConfiguration(anyString()))
        .thenReturn(DelegateConfiguration.builder().delegateVersions(singletonList(VERSION)).build());

    Delegate deletedDelegate = createDelegateBuilder().build();
    deletedDelegate.setAccountId(accountId);
    deletedDelegate.setStatus(DelegateInstanceStatus.DELETED);

    Delegate delegateWithoutScalingGroup = createDelegateBuilder().build();
    delegateWithoutScalingGroup.setAccountId(accountId);

    persistence.save(Arrays.asList(delegateWithoutScalingGroup, deletedDelegate));
    delegateService.registerHeartbeat(accountId, delegateWithoutScalingGroup.getUuid(),
        DelegateConnectionHeartbeat.builder().delegateConnectionId(generateUuid()).version(VERSION).build(),
        ConnectionMode.POLLING);

    DelegateStatus delegateStatus = delegateService.getDelegateStatusWithScalingGroups(accountId);

    assertThat(delegateStatus.getPublishedVersions()).hasSize(1).contains(VERSION);
    assertThat(delegateStatus.getDelegates()).hasSize(1);
    assertThat(delegateStatus.getScalingGroups()).hasSize(0);
    validateDelegateInnerProperties(delegateWithoutScalingGroup.getUuid(), delegateStatus.getDelegates().get(0));
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldGetDelegateStatus2ScalingGroupHasCorrectItems() {
    String accountId = generateUuid();
    when(accountService.getDelegateConfiguration(anyString()))
        .thenReturn(DelegateConfiguration.builder().delegateVersions(singletonList(VERSION)).build());

    Delegate deletedDelegate = createDelegateBuilder().build();
    deletedDelegate.setAccountId(accountId);
    deletedDelegate.setStatus(DelegateInstanceStatus.DELETED);

    // these three delegates should be returned
    Delegate delegateWithScalingGroup1 = createDelegateBuilder().build();
    delegateWithScalingGroup1.setAccountId(accountId);
    delegateWithScalingGroup1.setDelegateGroupName("test1");
    Delegate delegateWithScalingGroup2 = createDelegateBuilder().build();
    delegateWithScalingGroup2.setAccountId(accountId);
    delegateWithScalingGroup2.setDelegateGroupName("test1");
    Delegate delegateWithScalingGroup3 = createDelegateBuilder().build();
    delegateWithScalingGroup3.setAccountId(accountId);
    delegateWithScalingGroup3.setDelegateGroupName("test2");
    // these two delegates should not appear.
    Delegate delegateWithScalingGroup4 = createDelegateBuilder().build();
    delegateWithScalingGroup4.setAccountId(accountId);
    delegateWithScalingGroup4.setDelegateGroupName("test2");
    // this delegate should cause an empty group to be returned
    Delegate delegateWithScalingGroup5 = createDelegateBuilder().build();
    delegateWithScalingGroup5.setAccountId(accountId);
    delegateWithScalingGroup5.setDelegateGroupName("test3");

    persistence.save(Arrays.asList(deletedDelegate, delegateWithScalingGroup1, delegateWithScalingGroup2,
        delegateWithScalingGroup3, delegateWithScalingGroup4, delegateWithScalingGroup5));
    delegateService.registerHeartbeat(accountId, delegateWithScalingGroup1.getUuid(),
        DelegateConnectionHeartbeat.builder().delegateConnectionId(generateUuid()).version(VERSION).build(),
        ConnectionMode.POLLING);
    delegateService.registerHeartbeat(accountId, delegateWithScalingGroup2.getUuid(),
        DelegateConnectionHeartbeat.builder().delegateConnectionId(generateUuid()).version(VERSION).build(),
        ConnectionMode.POLLING);
    delegateService.registerHeartbeat(accountId, delegateWithScalingGroup3.getUuid(),
        DelegateConnectionHeartbeat.builder().delegateConnectionId(generateUuid()).version(VERSION).build(),
        ConnectionMode.POLLING);

    DelegateStatus delegateStatus = delegateService.getDelegateStatusWithScalingGroups(accountId);

    assertThat(delegateStatus.getPublishedVersions()).hasSize(1).contains(VERSION);

    assertThat(delegateStatus.getScalingGroups()).hasSize(3);
    assertThat(delegateStatus.getScalingGroups())
        .extracting(DelegateScalingGroup::getGroupName)
        .containsOnly("test1", "test2", "test3");

    for (DelegateScalingGroup group : delegateStatus.getScalingGroups()) {
      if (group.getGroupName().equals("test1")) {
        assertThat(group.getDelegates()).hasSize(2);
        assertThat(group.getDelegates())
            .extracting(DelegateStatus.DelegateInner::getUuid)
            .containsOnly(delegateWithScalingGroup1.getUuid(), delegateWithScalingGroup2.getUuid());
      } else if (group.getGroupName().equals("test2")) {
        assertThat(group.getDelegates()).hasSize(1);
        assertThat(group.getDelegates().get(0).getUuid()).isEqualTo(delegateWithScalingGroup3.getUuid());
      } else if (group.getGroupName().equals("test3")) {
        assertThat(group.getDelegates()).hasSize(0);
      }
    }
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldGetDelegateStatus2ScalingGroupEmpty() {
    String accountId = generateUuid();
    when(accountService.getDelegateConfiguration(anyString()))
        .thenReturn(DelegateConfiguration.builder().delegateVersions(singletonList(VERSION)).build());

    Delegate deletedDelegate = createDelegateBuilder().build();
    deletedDelegate.setAccountId(accountId);
    deletedDelegate.setStatus(DelegateInstanceStatus.DELETED);
    deletedDelegate.setDelegateGroupName("test");
    persistence.save(deletedDelegate);

    DelegateStatus status = delegateService.getDelegateStatusWithScalingGroups(accountId);

    assertThat(status.getScalingGroups()).isNotNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldUpdate() {
    String accountId = generateUuid();
    String delegateProfileId = generateUuid();

    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(accountId);
    persistence.save(delegate);
    delegate.setLastHeartBeat(System.currentTimeMillis());
    delegate.setStatus(DelegateInstanceStatus.DISABLED);
    delegate.setDelegateProfileId(delegateProfileId);
    delegateService.update(delegate);
    Delegate updatedDelegate = persistence.get(Delegate.class, delegate.getUuid());
    assertThat(updatedDelegate).isEqualToIgnoringGivenFields(delegate, DelegateKeys.validUntil);
    verify(eventEmitter)
        .send(Channel.DELEGATES,
            anEvent().withOrgId(accountId).withUuid(delegate.getUuid()).withType(Type.UPDATE).build());
    verify(delegateProfileSubject).fireInform(any(), eq(accountId), eq(delegate.getUuid()), eq(delegateProfileId));
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testUpdateApprovalStatusShouldSetStatusToEnabled() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder().build();
    existingDelegate.setUuid(delegateId);
    existingDelegate.setAccountId(accountId);
    existingDelegate.setStatus(DelegateInstanceStatus.WAITING_FOR_APPROVAL);
    persistence.save(existingDelegate);

    Delegate updatedDelegate = delegateService.updateApprovalStatus(accountId, delegateId, DelegateApproval.ACTIVATE);

    assertThat(existingDelegate).isEqualToIgnoringGivenFields(updatedDelegate, DelegateKeys.status);
    assertThat(DelegateInstanceStatus.ENABLED).isEqualTo(updatedDelegate.getStatus());
    verify(auditServiceHelper)
        .reportForAuditingUsingAccountId(accountId, existingDelegate, updatedDelegate, Type.DELEGATE_APPROVAL);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testUpdateApprovalStatusShouldSetStatusToDeleted() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    Delegate existingDelegate = createDelegateBuilder().build();
    existingDelegate.setUuid(delegateId);
    existingDelegate.setAccountId(accountId);
    existingDelegate.setStatus(DelegateInstanceStatus.WAITING_FOR_APPROVAL);
    persistence.save(existingDelegate);

    Delegate updatedDelegate = delegateService.updateApprovalStatus(accountId, delegateId, DelegateApproval.REJECT);

    assertThat(existingDelegate).isEqualToIgnoringGivenFields(updatedDelegate, DelegateKeys.status);
    assertThat(DelegateInstanceStatus.DELETED).isEqualTo(updatedDelegate.getStatus());
    verify(auditServiceHelper)
        .reportForAuditingUsingAccountId(accountId, existingDelegate, updatedDelegate, Type.DELEGATE_REJECTION);
    verify(broadcaster).broadcast(SELF_DESTRUCT + delegateId);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldUpdateEcs() {
    String accountId = generateUuid();
    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(accountId);
    delegate.setDelegateType(ECS);
    persistence.save(delegate);
    delegate.setLastHeartBeat(System.currentTimeMillis());
    delegate.setStatus(DelegateInstanceStatus.DISABLED);
    delegateService.update(delegate);
    Delegate updatedDelegate = persistence.get(Delegate.class, delegate.getUuid());
    assertThat(updatedDelegate).isEqualToIgnoringGivenFields(delegate, DelegateKeys.validUntil);
    verify(eventEmitter)
        .send(Channel.DELEGATES,
            anEvent().withOrgId(accountId).withUuid(delegate.getUuid()).withType(Type.UPDATE).build());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldAdd() {
    String accountId = generateUuid();
    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(accountId);
    delegate.setUuid(generateUuid());

    DelegateProfile delegateProfile =
        createDelegateProfileBuilder().accountId(delegate.getAccountId()).uuid(generateUuid()).build();
    delegate.setDelegateProfileId(delegateProfile.getUuid());

    when(delegateProfileService.get(delegate.getAccountId(), delegateProfile.getUuid())).thenReturn(delegateProfile);

    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    delegate = delegateService.add(delegate);

    assertThat(persistence.get(Delegate.class, delegate.getUuid())).isEqualTo(delegate);
    verify(eventEmitter)
        .send(Channel.DELEGATES,
            anEvent().withOrgId(accountId).withUuid(delegate.getUuid()).withType(Type.CREATE).build());
    verify(capabilityService, never()).getAllCapabilityRequirements(accountId);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldAddAndInvokeRegenerateCapabilityPermissions() {
    featureTestHelper.enableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
    String accountId = generateUuid();
    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(accountId);
    delegate.setUuid(generateUuid());

    DelegateProfile delegateProfile =
        createDelegateProfileBuilder().accountId(delegate.getAccountId()).uuid(generateUuid()).build();
    delegate.setDelegateProfileId(delegateProfile.getUuid());

    when(delegateProfileService.get(delegate.getAccountId(), delegateProfile.getUuid())).thenReturn(delegateProfile);

    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    delegate = delegateService.add(delegate);

    assertThat(persistence.get(Delegate.class, delegate.getUuid())).isEqualTo(delegate);
    verify(eventEmitter)
        .send(Channel.DELEGATES,
            anEvent().withOrgId(accountId).withUuid(delegate.getUuid()).withType(Type.CREATE).build());
    verify(capabilityService).getAllCapabilityRequirements(accountId);
    featureTestHelper.disableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldAddWithWaitingForApprovalStatus() {
    String accountId = generateUuid();
    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(accountId);
    delegate.setUuid(generateUuid());

    DelegateProfile delegateProfile = createDelegateProfileBuilder()
                                          .accountId(delegate.getAccountId())
                                          .uuid(generateUuid())
                                          .approvalRequired(true)
                                          .build();
    delegate.setDelegateProfileId(delegateProfile.getUuid());

    when(delegateProfileService.get(delegate.getAccountId(), delegateProfile.getUuid())).thenReturn(delegateProfile);

    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    delegate = delegateService.add(delegate);

    assertThat(delegate).isEqualToIgnoringGivenFields(delegate, DelegateKeys.status);
    assertThat(delegate.getStatus()).isEqualTo(DelegateInstanceStatus.WAITING_FOR_APPROVAL);
    verify(eventEmitter)
        .send(Channel.DELEGATES,
            anEvent().withOrgId(accountId).withUuid(delegate.getUuid()).withType(Type.CREATE).build());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldAddWithPrimaryProfile() {
    String accountId = generateUuid();
    Delegate delegateWithoutProfile = createDelegateBuilder().build();
    delegateWithoutProfile.setAccountId(accountId);
    delegateWithoutProfile.setUuid(generateUuid());

    DelegateProfile primaryDelegateProfile =
        createDelegateProfileBuilder().accountId(delegateWithoutProfile.getAccountId()).primary(true).build();
    when(delegateProfileService.fetchCgPrimaryProfile(delegateWithoutProfile.getAccountId()))
        .thenReturn(primaryDelegateProfile);

    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    delegateWithoutProfile = delegateService.add(delegateWithoutProfile);

    Delegate savedDelegate = persistence.get(Delegate.class, delegateWithoutProfile.getUuid());
    assertThat(savedDelegate).isEqualToIgnoringGivenFields(delegateWithoutProfile, DelegateKeys.delegateProfileId);
    assertThat(savedDelegate.getDelegateProfileId()).isEqualTo(primaryDelegateProfile.getUuid());
    verify(eventEmitter)
        .send(Channel.DELEGATES,
            anEvent().withOrgId(accountId).withUuid(delegateWithoutProfile.getUuid()).withType(Type.CREATE).build());

    Delegate delegateWithNonExistingProfile = createDelegateBuilder().build();
    delegateWithNonExistingProfile.setAccountId(accountId);
    delegateWithNonExistingProfile.setUuid(generateUuid());
    delegateWithNonExistingProfile.setDelegateProfileId("nonExistingProfile");
    when(delegateProfileService.get(
             delegateWithoutProfile.getAccountId(), delegateWithNonExistingProfile.getDelegateProfileId()))
        .thenReturn(null);

    delegateWithNonExistingProfile = delegateService.add(delegateWithNonExistingProfile);

    savedDelegate = persistence.get(Delegate.class, delegateWithNonExistingProfile.getUuid());
    assertThat(savedDelegate)
        .isEqualToIgnoringGivenFields(delegateWithNonExistingProfile, DelegateKeys.delegateProfileId);
    assertThat(savedDelegate.getDelegateProfileId()).isEqualTo(primaryDelegateProfile.getUuid());

    // Test Ng primary profile
    Delegate ngDelegateWithoutProfile = createDelegateBuilder().build();
    ngDelegateWithoutProfile.setAccountId(accountId);
    ngDelegateWithoutProfile.setUuid(generateUuid());
    ngDelegateWithoutProfile.setNg(true);

    DelegateProfile ngPrimaryDelegateProfile = createDelegateProfileBuilder()
                                                   .accountId(ngDelegateWithoutProfile.getAccountId())
                                                   .ng(true)
                                                   .primary(true)
                                                   .build();
    when(delegateProfileService.fetchNgPrimaryProfile(ngDelegateWithoutProfile.getAccountId()))
        .thenReturn(ngPrimaryDelegateProfile);

    ngDelegateWithoutProfile = delegateService.add(ngDelegateWithoutProfile);

    savedDelegate = persistence.get(Delegate.class, ngDelegateWithoutProfile.getUuid());
    assertThat(savedDelegate.getDelegateProfileId()).isEqualTo(ngPrimaryDelegateProfile.getUuid());
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void shouldNotAddMoreThanAllowedDelegates() {
    String accountId = generateUuid();
    int maxDelegatesAllowed = 1;
    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(maxDelegatesAllowed);

    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(accountId);
    DelegateProfile primaryDelegateProfile =
        createDelegateProfileBuilder().accountId(delegate.getAccountId()).primary(true).build();

    delegate.setDelegateProfileId(primaryDelegateProfile.getUuid());
    when(delegateProfileService.fetchCgPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);

    IntStream.range(0, maxDelegatesAllowed).forEach(i -> delegateService.add(delegate));
    try {
      Delegate maxUsageDelegate = createDelegateBuilder().build();
      maxUsageDelegate.setAccountId(accountId);
      delegateService.add(delegate);
      fail("");
    } catch (WingsException ignored) {
    }
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldDelete() {
    String id = persistence.save(createDelegateBuilder().build());
    delegateService.delete(ACCOUNT_ID, id);
    assertThat(persistence.createQuery(Delegate.class).filter(DelegateKeys.accountId, ACCOUNT_ID).asList()).hasSize(0);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldRegister() {
    String accountId = generateUuid();
    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(accountId);
    DelegateProfile primaryDelegateProfile =
        createDelegateProfileBuilder().accountId(delegate.getAccountId()).primary(true).build();

    delegate.setDelegateProfileId(primaryDelegateProfile.getUuid());
    when(delegateProfileService.fetchCgPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);
    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    DelegateRegisterResponse registerResponse = delegateService.register(delegate);
    Delegate delegateFromDb = delegateCache.get(accountId, registerResponse.getDelegateId(), true);
    assertThat(delegateFromDb).isEqualTo(delegate);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldRegisterDelegateParams() {
    String accountId = generateUuid();

    DelegateSizeDetails sizeDetails = DelegateSizeDetails.builder()
                                          .size(DelegateSize.LAPTOP)
                                          .label("Laptop")
                                          .replicas(1)
                                          .taskLimit(50)
                                          .cpu(0.5)
                                          .ram(1650)
                                          .build();

    DelegateParams params = DelegateParams.builder()
                                .accountId(accountId)
                                .sessionIdentifier("sessionId")
                                .delegateSize(DelegateSize.LAPTOP.name())
                                .hostName(HOST_NAME)
                                .description(DESCRIPTION)
                                .delegateType(DOCKER_DELEGATE)
                                .ip("127.0.0.1")
                                .delegateGroupName(DELEGATE_GROUP_NAME)
                                .delegateGroupId(generateUuid())
                                .version(VERSION)
                                .proxy(true)
                                .pollingModeEnabled(true)
                                .sampleDelegate(true)
                                .build();

    DelegateProfile profile = createDelegateProfileBuilder().accountId(accountId).primary(true).build();
    when(delegateProfileService.fetchNgPrimaryProfile(accountId)).thenReturn(profile);
    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    DelegateRegisterResponse registerResponse = delegateService.register(params);
    Delegate delegateFromDb = delegateCache.get(accountId, registerResponse.getDelegateId(), true);

    assertThat(delegateFromDb.getAccountId()).isEqualTo(params.getAccountId());
    assertThat(delegateFromDb.getSessionIdentifier()).isEqualTo(params.getSessionIdentifier());
    assertThat(delegateFromDb.isNg()).isTrue();
    assertThat(delegateFromDb.getSizeDetails()).isEqualTo(sizeDetails);
    assertThat(delegateFromDb.getHostName()).isEqualTo(params.getHostName());
    assertThat(delegateFromDb.getDescription()).isEqualTo(params.getDescription());
    assertThat(delegateFromDb.getDelegateType()).isEqualTo(params.getDelegateType());
    assertThat(delegateFromDb.getIp()).isEqualTo(params.getIp());
    assertThat(delegateFromDb.getDelegateGroupName()).isEqualTo(params.getDelegateGroupName());
    assertThat(delegateFromDb.getDelegateGroupId()).isEqualTo(params.getDelegateGroupId());
    assertThat(delegateFromDb.getVersion()).isEqualTo(params.getVersion());
    assertThat(delegateFromDb.isProxy()).isEqualTo(params.isProxy());
    assertThat(delegateFromDb.isPolllingModeEnabled()).isEqualTo(params.isPollingModeEnabled());
    assertThat(delegateFromDb.isSampleDelegate()).isEqualTo(params.isSampleDelegate());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldRegisterDelegateParamsWithOrgId() {
    String accountId = generateUuid();

    DelegateParams params = DelegateParams.builder()
                                .accountId(accountId)
                                .sessionIdentifier("sessionId")
                                .orgIdentifier("orgId")
                                .hostName(HOST_NAME)
                                .description(DESCRIPTION)
                                .delegateType(DOCKER_DELEGATE)
                                .ip("127.0.0.1")
                                .delegateGroupName(DELEGATE_GROUP_NAME)
                                .delegateGroupId(generateUuid())
                                .version(VERSION)
                                .proxy(true)
                                .pollingModeEnabled(true)
                                .sampleDelegate(true)
                                .build();

    DelegateProfile profile = createDelegateProfileBuilder().accountId(accountId).primary(true).build();
    when(delegateProfileService.fetchNgPrimaryProfile(accountId)).thenReturn(profile);
    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    DelegateRegisterResponse registerResponse = delegateService.register(params);
    Delegate delegateFromDb = delegateCache.get(accountId, registerResponse.getDelegateId(), true);

    assertThat(delegateFromDb.getOwner().getIdentifier()).isEqualTo("orgId");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldRegisterDelegateParamsWithProjectId() {
    String accountId = generateUuid();

    DelegateParams params = DelegateParams.builder()
                                .accountId(accountId)
                                .sessionIdentifier("sessionId")
                                .orgIdentifier("orgId")
                                .projectIdentifier("projectId")
                                .hostName(HOST_NAME)
                                .description(DESCRIPTION)
                                .delegateType(DOCKER_DELEGATE)
                                .ip("127.0.0.1")
                                .delegateGroupName(DELEGATE_GROUP_NAME)
                                .delegateGroupId(generateUuid())
                                .version(VERSION)
                                .proxy(true)
                                .pollingModeEnabled(true)
                                .sampleDelegate(true)
                                .build();

    DelegateProfile profile = createDelegateProfileBuilder().accountId(accountId).primary(true).build();
    when(delegateProfileService.fetchNgPrimaryProfile(accountId)).thenReturn(profile);
    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    DelegateRegisterResponse registerResponse = delegateService.register(params);
    Delegate delegateFromDb = delegateCache.get(accountId, registerResponse.getDelegateId(), true);

    assertThat(delegateFromDb.getOwner().getIdentifier()).isEqualTo("orgId/projectId");
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldRegisterExistingDelegateParams() {
    String accountId = generateUuid();
    DelegateParams params = DelegateParams.builder()
                                .accountId(accountId)
                                .hostName(HOST_NAME)
                                .description(DESCRIPTION)
                                .delegateType(DOCKER_DELEGATE)
                                .ip("127.0.0.1")
                                .delegateGroupName(DELEGATE_GROUP_NAME)
                                .version(VERSION)
                                .proxy(true)
                                .pollingModeEnabled(true)
                                .sampleDelegate(false)
                                .build();
    DelegateProfile profile = createDelegateProfileBuilder().accountId(accountId).primary(true).build();
    when(delegateProfileService.fetchCgPrimaryProfile(accountId)).thenReturn(profile);
    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    DelegateRegisterResponse registerResponse = delegateService.register(params);
    Delegate delegateFromDb = delegateCache.get(accountId, registerResponse.getDelegateId(), true);

    assertThat(delegateFromDb.getAccountId()).isEqualTo(params.getAccountId());
    assertThat(delegateFromDb.getHostName()).isEqualTo(params.getHostName());
    assertThat(delegateFromDb.getDescription()).isEqualTo(params.getDescription());
    assertThat(delegateFromDb.getDelegateType()).isEqualTo(params.getDelegateType());
    assertThat(delegateFromDb.getIp()).isEqualTo(params.getIp());
    assertThat(delegateFromDb.getDelegateGroupName()).isEqualTo(params.getDelegateGroupName());
    assertThat(delegateFromDb.getDelegateGroupId()).isNotNull();
    assertThat(delegateFromDb.getVersion()).isEqualTo(params.getVersion());
    assertThat(delegateFromDb.isProxy()).isEqualTo(params.isProxy());
    assertThat(delegateFromDb.isPolllingModeEnabled()).isEqualTo(params.isPollingModeEnabled());
    assertThat(delegateFromDb.isSampleDelegate()).isEqualTo(params.isSampleDelegate());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldRegisterExistingDelegate() {
    String accountId = generateUuid();
    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(accountId);
    DelegateProfile primaryDelegateProfile =
        createDelegateProfileBuilder().accountId(delegate.getAccountId()).primary(true).build();

    delegate.setDelegateProfileId(primaryDelegateProfile.getUuid());
    when(delegateProfileService.fetchCgPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);

    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    delegate = delegateService.add(delegate);
    delegateService.register(delegate);
    Delegate registeredDelegate = delegateCache.get(accountId, delegate.getUuid(), true);
    assertThat(registeredDelegate).isEqualToIgnoringGivenFields(delegate, DelegateKeys.validUntil);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldRegisterParamsWithExistingDelegate() {
    String accountId = generateUuid();
    Delegate delegate = createDelegateBuilder()
                            .accountId(accountId)
                            .hostName(HOST_NAME)
                            .description(DESCRIPTION)
                            .delegateType(DOCKER_DELEGATE)
                            .ip("127.0.0.1")
                            .delegateGroupName(DELEGATE_GROUP_NAME)
                            .version(VERSION)
                            .proxy(false)
                            .polllingModeEnabled(false)
                            .sampleDelegate(false)
                            .build();
    DelegateProfile primaryDelegateProfile =
        createDelegateProfileBuilder().accountId(delegate.getAccountId()).primary(true).build();

    delegate.setDelegateProfileId(primaryDelegateProfile.getUuid());
    when(delegateProfileService.fetchCgPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);

    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    delegate = delegateService.add(delegate);
    DelegateParams params = DelegateParams.builder()
                                .delegateId(delegate.getUuid())
                                .accountId(accountId)
                                .hostName(HOST_NAME + "UPDATED")
                                .description(DESCRIPTION + "UPDATED")
                                .delegateType(DOCKER_DELEGATE + "UPDATED")
                                .ip("127.0.0.2")
                                .delegateGroupName(DELEGATE_GROUP_NAME + "UPDATED")
                                .version(VERSION + "UPDATED")
                                .proxy(true)
                                .pollingModeEnabled(true)
                                .sampleDelegate(false)
                                .build();

    delegateService.register(params);

    Delegate delegateFromDb = delegateCache.get(accountId, delegate.getUuid(), true);
    assertThat(delegateFromDb.getAccountId()).isEqualTo(params.getAccountId());
    assertThat(delegateFromDb.isNg()).isFalse();
    assertThat(delegateFromDb.getHostName()).isEqualTo(params.getHostName());
    assertThat(delegateFromDb.getDescription()).isEqualTo(params.getDescription());
    assertThat(delegateFromDb.getDelegateType()).isEqualTo(params.getDelegateType());
    assertThat(delegateFromDb.getIp()).isEqualTo(params.getIp());
    assertThat(delegateFromDb.getDelegateGroupName()).isEqualTo(params.getDelegateGroupName());
    assertThat(delegateFromDb.getVersion()).isEqualTo(params.getVersion());
    assertThat(delegateFromDb.isProxy()).isEqualTo(params.isProxy());
    assertThat(delegateFromDb.isPolllingModeEnabled()).isEqualTo(params.isPollingModeEnabled());
    assertThat(delegateFromDb.isSampleDelegate()).isEqualTo(params.isSampleDelegate());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldRegisterParamsWithExistingDelegateForECS() {
    String accountId = generateUuid();
    Delegate delegate = createDelegateBuilder()
                            .accountId(accountId)
                            .hostName(HOST_NAME)
                            .description(DESCRIPTION)
                            .delegateType(ECS)
                            .ip("127.0.0.1")
                            .delegateGroupName(DELEGATE_GROUP_NAME)
                            .version(VERSION)
                            .proxy(false)
                            .polllingModeEnabled(false)
                            .sampleDelegate(false)
                            .build();
    DelegateProfile primaryDelegateProfile =
        createDelegateProfileBuilder().accountId(delegate.getAccountId()).primary(true).build();

    delegate.setDelegateProfileId(primaryDelegateProfile.getUuid());
    when(delegateProfileService.fetchCgPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);

    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    delegate = delegateService.add(delegate);
    DelegateParams params = DelegateParams.builder()
                                .delegateId(delegate.getUuid())
                                .accountId(accountId)
                                .hostName(HOST_NAME)
                                .description(DESCRIPTION + "UPDATED")
                                .delegateType(ECS)
                                .ip("127.0.0.2")
                                .delegateGroupName(DELEGATE_GROUP_NAME + "UPDATED")
                                .delegateRandomToken("13")
                                .version(VERSION + "UPDATED")
                                .proxy(true)
                                .pollingModeEnabled(true)
                                .sampleDelegate(false)
                                .build();

    delegateService.register(params);

    Delegate delegateFromDb = delegateCache.get(accountId, delegate.getUuid(), true);

    assertThat(delegateFromDb.getAccountId()).isEqualTo(params.getAccountId());
    assertThat(delegateFromDb.getDescription()).isEqualTo(params.getDescription());
    assertThat(delegateFromDb.getDelegateType()).isEqualTo(params.getDelegateType());
    assertThat(delegateFromDb.getIp()).isEqualTo(params.getIp());
    assertThat(delegateFromDb.getDelegateGroupName()).isEqualTo(params.getDelegateGroupName());
    assertThat(delegateFromDb.getVersion()).isEqualTo(params.getVersion());
    assertThat(delegateFromDb.isProxy()).isEqualTo(params.isProxy());
    assertThat(delegateFromDb.isPolllingModeEnabled()).isEqualTo(params.isPollingModeEnabled());
    assertThat(delegateFromDb.isSampleDelegate()).isEqualTo(params.isSampleDelegate());
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldRegisterHeartbeatPolling() throws IllegalAccessException {
    DelegateConnectionDao mockConnectionDao = Mockito.mock(DelegateConnectionDao.class);
    when(mockConnectionDao.findAndDeletePreviousConnections(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(null);
    FieldUtils.writeField(delegateService, "delegateConnectionDao", mockConnectionDao, true);
    String delegateConnectionId = generateTimeBasedUuid();
    DelegateConnectionHeartbeat heartbeat =
        DelegateConnectionHeartbeat.builder().version(VERSION).delegateConnectionId(delegateConnectionId).build();
    delegateService.registerHeartbeat(ACCOUNT_ID, DELEGATE_ID, heartbeat, ConnectionMode.POLLING);
    verify(mockConnectionDao).findAndDeletePreviousConnections(ACCOUNT_ID, DELEGATE_ID, delegateConnectionId, VERSION);
    FieldUtils.writeField(delegateService, "delegateConnectionDao", delegateConnectionDao, true);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldRegisterHeartbeatWithPreviousDisconnection() throws IllegalAccessException {
    featureTestHelper.enableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
    DelegateConnection previousDelegateConnection = mock(DelegateConnection.class);
    when(previousDelegateConnection.isDisconnected()).thenReturn(true);
    DelegateConnectionDao mockConnectionDao = mock(DelegateConnectionDao.class);
    when(mockConnectionDao.upsertCurrentConnection(any(), any(), any(), any(), any()))
        .thenReturn(previousDelegateConnection);
    FieldUtils.writeField(delegateService, "delegateConnectionDao", mockConnectionDao, true);
    String delegateConnectionId = generateTimeBasedUuid();
    DelegateConnectionHeartbeat heartbeat =
        DelegateConnectionHeartbeat.builder().version(VERSION).delegateConnectionId(delegateConnectionId).build();

    delegateService.registerHeartbeat(ACCOUNT_ID, DELEGATE_ID, heartbeat, null);

    verify(subject).fireInform(any(), eq(ACCOUNT_ID), eq(DELEGATE_ID));
    FieldUtils.writeField(delegateService, "delegateConnectionDao", delegateConnectionDao, true);
    featureTestHelper.disableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldRegisterHeartbeatStreaming() throws IllegalAccessException {
    DelegateConnectionDao mockConnectionDao = Mockito.mock(DelegateConnectionDao.class);
    when(mockConnectionDao.findAndDeletePreviousConnections(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(null);
    FieldUtils.writeField(delegateService, "delegateConnectionDao", mockConnectionDao, true);
    String delegateConnectionId = generateTimeBasedUuid();
    DelegateConnectionHeartbeat heartbeat =
        DelegateConnectionHeartbeat.builder().version(VERSION).delegateConnectionId(delegateConnectionId).build();
    delegateService.registerHeartbeat(ACCOUNT_ID, DELEGATE_ID, heartbeat, ConnectionMode.STREAMING);
    verify(mockConnectionDao).findAndDeletePreviousConnections(ACCOUNT_ID, DELEGATE_ID, delegateConnectionId, VERSION);
    FieldUtils.writeField(delegateService, "delegateConnectionDao", delegateConnectionDao, true);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldRegisterHeartbeatSendSelfDestruct() throws IllegalAccessException, InterruptedException {
    Delegate delegate = createDelegateBuilder()
                            .accountId(ACCOUNT_ID)
                            .uuid(DELEGATE_ID)
                            .hostName(HOST_NAME)
                            .description(DESCRIPTION)
                            .delegateType(ECS)
                            .ip("127.0.0.1")
                            .delegateGroupName(DELEGATE_GROUP_NAME)
                            .version(VERSION)
                            .proxy(false)
                            .polllingModeEnabled(false)
                            .sampleDelegate(false)
                            .build();
    DelegateProfile primaryDelegateProfile =
        createDelegateProfileBuilder().accountId(delegate.getAccountId()).primary(true).build();

    delegate.setDelegateProfileId(primaryDelegateProfile.getUuid());
    when(delegatesFeature.getMaxUsageAllowedForAccount(ACCOUNT_ID)).thenReturn(Integer.MAX_VALUE);
    when(delegateProfileService.fetchCgPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);
    delegateService.add(delegate);
    DelegateConnectionDao mockConnectionDao = Mockito.mock(DelegateConnectionDao.class);
    FieldUtils.writeField(delegateService, "delegateConnectionDao", mockConnectionDao, true);
    when(broadcasterFactory.lookup(anyString(), eq(true))).thenReturn(broadcaster);
    String delegateConnectionId = generateTimeBasedUuid();
    Thread.sleep(2L);
    String newerDelegateConnectionId = generateTimeBasedUuid();
    DelegateConnection newerExistingConnection = DelegateConnection.builder().uuid(newerDelegateConnectionId).build();
    DelegateConnectionHeartbeat heartbeat =
        DelegateConnectionHeartbeat.builder().version(VERSION).delegateConnectionId(delegateConnectionId).build();
    when(mockConnectionDao.findAndDeletePreviousConnections(ACCOUNT_ID, DELEGATE_ID, delegateConnectionId, VERSION))
        .thenReturn(newerExistingConnection);
    delegateService.registerHeartbeat(ACCOUNT_ID, DELEGATE_ID, heartbeat, ConnectionMode.STREAMING);
    verify(mockConnectionDao).findAndDeletePreviousConnections(ACCOUNT_ID, DELEGATE_ID, delegateConnectionId, VERSION);
    verify(mockConnectionDao).replaceWithNewerConnection(delegateConnectionId, newerExistingConnection);
    verify(broadcaster).broadcast(SELF_DESTRUCT + DELEGATE_ID + "-" + delegateConnectionId);
    FieldUtils.writeField(delegateService, "delegateConnectionDao", delegateConnectionDao, true);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldRegisterHeartbeatThrowException() throws IllegalAccessException, InterruptedException {
    try {
      thrown.expect(DuplicateDelegateException.class);
      Delegate delegate = createDelegateBuilder()
                              .accountId(ACCOUNT_ID)
                              .uuid(DELEGATE_ID)
                              .hostName(HOST_NAME)
                              .description(DESCRIPTION)
                              .delegateType(ECS)
                              .ip("127.0.0.1")
                              .delegateGroupName(DELEGATE_GROUP_NAME)
                              .version(VERSION)
                              .proxy(false)
                              .polllingModeEnabled(false)
                              .sampleDelegate(false)
                              .build();
      DelegateProfile primaryDelegateProfile =
          createDelegateProfileBuilder().accountId(delegate.getAccountId()).primary(true).build();

      delegate.setDelegateProfileId(primaryDelegateProfile.getUuid());
      when(delegatesFeature.getMaxUsageAllowedForAccount(ACCOUNT_ID)).thenReturn(Integer.MAX_VALUE);
      when(delegateProfileService.fetchCgPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);
      delegateService.add(delegate);

      DelegateConnectionDao mockConnectionDao = Mockito.mock(DelegateConnectionDao.class);
      FieldUtils.writeField(delegateService, "delegateConnectionDao", mockConnectionDao, true);
      String delegateConnectionId = generateTimeBasedUuid();
      Thread.sleep(2L);
      String newerDelegateConnectionId = generateTimeBasedUuid();
      DelegateConnection newerExistingConnection =
          DelegateConnection.builder().uuid(newerDelegateConnectionId).location("/location1").build();
      when(mockConnectionDao.findAndDeletePreviousConnections(ACCOUNT_ID, DELEGATE_ID, delegateConnectionId, VERSION))
          .thenReturn(newerExistingConnection);
      DelegateConnectionHeartbeat heartbeat = DelegateConnectionHeartbeat.builder()
                                                  .version(VERSION)
                                                  .delegateConnectionId(delegateConnectionId)
                                                  .location("/location2")
                                                  .build();

      delegateService.registerHeartbeat(ACCOUNT_ID, DELEGATE_ID, heartbeat, ConnectionMode.POLLING);
      verify(mockConnectionDao)
          .findAndDeletePreviousConnections(ACCOUNT_ID, DELEGATE_ID, delegateConnectionId, VERSION);
      verify(mockConnectionDao).replaceWithNewerConnection(delegateConnectionId, newerExistingConnection);
    } finally {
      FieldUtils.writeField(delegateService, "delegateConnectionDao", delegateConnectionDao, true);
    }
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotFailOnNullRegisterParamsWithExistingDelegate() {
    String accountId = generateUuid();
    Delegate delegate = createDelegateBuilder()
                            .accountId(accountId)
                            .hostName(HOST_NAME)
                            .description(DESCRIPTION)
                            .delegateType(DOCKER_DELEGATE)
                            .ip("127.0.0.1")
                            .delegateGroupName(DELEGATE_GROUP_NAME)
                            .version(VERSION)
                            .proxy(false)
                            .polllingModeEnabled(false)
                            .sampleDelegate(false)
                            .build();
    DelegateProfile primaryDelegateProfile =
        createDelegateProfileBuilder().accountId(delegate.getAccountId()).primary(true).build();

    delegate.setDelegateProfileId(primaryDelegateProfile.getUuid());
    when(delegateProfileService.fetchCgPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);

    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    delegate = delegateService.add(delegate);
    DelegateParams params = DelegateParams.builder()
                                .delegateId(delegate.getUuid())
                                .accountId(accountId)
                                .hostName(HOST_NAME + "UPDATED")
                                .version(VERSION + "UPDATED")
                                .proxy(true)
                                .pollingModeEnabled(true)
                                .sampleDelegate(false)
                                .build();

    delegateService.register(params);

    Delegate delegateFromDb = delegateCache.get(accountId, delegate.getUuid(), true);
    assertThat(delegateFromDb.getAccountId()).isEqualTo(params.getAccountId());
    assertThat(delegateFromDb.getHostName()).isEqualTo(params.getHostName());
    assertThat(delegateFromDb.getDescription()).isEqualTo(params.getDescription());
    assertThat(delegateFromDb.getDelegateType()).isEqualTo(params.getDelegateType());
    assertThat(delegateFromDb.getIp()).isEqualTo(params.getIp());
    assertThat(delegateFromDb.getDelegateGroupName()).isEqualTo(params.getDelegateGroupName());
    assertThat(delegateFromDb.getVersion()).isEqualTo(params.getVersion());
    assertThat(delegateFromDb.isProxy()).isEqualTo(params.isProxy());
    assertThat(delegateFromDb.isPolllingModeEnabled()).isEqualTo(params.isPollingModeEnabled());
    assertThat(delegateFromDb.isSampleDelegate()).isEqualTo(params.isSampleDelegate());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotRegisterExistingDelegateForDeletedAccount() {
    String accountId = generateUuid();
    Delegate delegate = Delegate.builder()
                            .accountId(accountId)
                            .ip("127.0.0.1")
                            .hostName("localhost")
                            .version(VERSION)
                            .status(DelegateInstanceStatus.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .build();

    DelegateProfile primaryDelegateProfile =
        createDelegateProfileBuilder().accountId(delegate.getAccountId()).primary(true).build();

    delegate.setDelegateProfileId(primaryDelegateProfile.getUuid());
    when(delegateProfileService.fetchCgPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);

    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    delegate = delegateService.add(delegate);
    when(licenseService.isAccountDeleted(accountId)).thenReturn(true);

    DelegateRegisterResponse registerResponse = delegateService.register(delegate);
    assertThat(registerResponse.getAction()).isEqualTo(DelegateRegisterResponse.Action.SELF_DESTRUCT);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotRegisterNewDelegateForDeletedAccount() {
    DelegateParams delegateParams = DelegateParams.builder()
                                        .accountId("DELETED_ACCOUNT")
                                        .ip("127.0.0.1")
                                        .hostName("localhost")
                                        .version(VERSION)
                                        .lastHeartBeat(System.currentTimeMillis())
                                        .build();
    when(licenseService.isAccountDeleted("DELETED_ACCOUNT")).thenReturn(true);
    DelegateRegisterResponse registerResponse = delegateService.register(delegateParams);
    assertThat(registerResponse.getAction()).isEqualTo(DelegateRegisterResponse.Action.SELF_DESTRUCT);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void shouldGetDelegateTaskEvents() {
    String delegateId = generateUuid();
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    List<DelegateTaskEvent> delegateTaskEvents = delegateService.getDelegateTaskEvents(ACCOUNT_ID, delegateId, false);
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

    List<DelegateTaskEvent> delegateTaskEvents = delegateService.getDelegateTaskEvents(ACCOUNT_ID, delegateId, false);
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
    delegateService.queueTask(delegateTask);
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
    delegateService.queueTask(delegateTask);
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

    delegateService.queueTask(delegateTask);
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

    assertThatThrownBy(() -> delegateService.queueTask(delegateTask))
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

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void processDelegateTaskResponseShouldRequeueTask() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .waitId(generateUuid())
                                    .delegateId(DELEGATE_ID)
                                    .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
                                    .version(VERSION)
                                    .data(TaskData.builder()
                                              .async(true)
                                              .taskType(TaskType.HTTP.name())
                                              .parameters(new Object[] {})
                                              .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                              .build())
                                    .tags(new ArrayList<>())
                                    .build();
    persistence.save(delegateTask);

    when(assignDelegateService.connectedWhitelistedDelegates(any())).thenReturn(asList("delegate1", "delegate2"));

    RetryDelegate retryDelegate = RetryDelegate.builder().retryPossible(true).delegateTask(delegateTask).build();
    when(retryObserverSubject.fireProcess(any(), any())).thenReturn(retryDelegate);

    delegateTaskService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build())
            .responseCode(ResponseCode.RETRY_ON_OTHER_DELEGATE)
            .build());

    DelegateTask updatedDelegateTask =
        persistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.uuid, delegateTask.getUuid()).get();

    assertThat(updatedDelegateTask).isNotNull();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldNotRequeueTaskWhenAfterDelegatesAreTried() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .waitId(generateUuid())
                                    .delegateId(DELEGATE_ID)
                                    .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
                                    .version(VERSION)
                                    .data(TaskData.builder()
                                              .async(true)
                                              .taskType(TaskType.HTTP.name())
                                              .parameters(new Object[] {})
                                              .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                              .build())
                                    .tags(new ArrayList<>())
                                    .build();
    persistence.save(delegateTask);

    when(assignDelegateService.connectedWhitelistedDelegates(any())).thenReturn(asList(DELEGATE_ID));

    RetryDelegate retryDelegate = RetryDelegate.builder().retryPossible(false).build();
    when(retryObserverSubject.fireProcess(any(), any())).thenReturn(retryDelegate);

    delegateTaskService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build())
            .responseCode(ResponseCode.RETRY_ON_OTHER_DELEGATE)
            .build());

    DelegateTask updatedDelegateTask =
        persistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.uuid, delegateTask.getUuid()).get();

    assertThat(updatedDelegateTask).isEqualTo(null);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldDownloadScripts() throws IOException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    when(delegateProfileService.get(ACCOUNT_ID, DELEGATE_PROFILE_ID))
        .thenReturn(createDelegateProfileBuilder().build());
    File gzipFile = delegateService.downloadScripts(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, DELEGATE_NAME, DELEGATE_PROFILE_ID);
    verifyDownloadScriptsResult(gzipFile, "/expectedStartOpenJdk.sh", "/expectedDelegateOpenJdk.sh");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldDownloadScriptsWithPrimaryProfile() throws IOException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    when(delegateProfileService.fetchCgPrimaryProfile(ACCOUNT_ID))
        .thenReturn(createDelegateProfileBuilder().uuid(DELEGATE_PROFILE_ID).build());
    File gzipFile = delegateService.downloadScripts(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, DELEGATE_NAME, null);
    verifyDownloadScriptsResult(gzipFile, "/expectedStartOpenJdk.sh", "/expectedDelegateOpenJdk.sh");

    when(delegateProfileService.get(ACCOUNT_ID, "invalidProfile")).thenReturn(null);
    when(delegateProfileService.fetchCgPrimaryProfile(ACCOUNT_ID))
        .thenReturn(createDelegateProfileBuilder().uuid(DELEGATE_PROFILE_ID).build());
    gzipFile = delegateService.downloadScripts(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, DELEGATE_NAME, "invalidProfile");
    verifyDownloadScriptsResult(gzipFile, "/expectedStartOpenJdk.sh", "/expectedDelegateOpenJdk.sh");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldDownloadScriptsWithoutDelegateName() throws IOException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    when(delegateProfileService.get(ACCOUNT_ID, DELEGATE_PROFILE_ID))
        .thenReturn(createDelegateProfileBuilder().build());
    File gzipFile = delegateService.downloadScripts(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, "", DELEGATE_PROFILE_ID);
    verifyDownloadScriptsResult(
        gzipFile, "/expectedStartWithoutDelegateName.sh", "/expectedDelegateWithoutDelegateName.sh");
  }

  private void verifyDownloadScriptsResult(File gzipFile, String expectedStartFilepath, String expectedDelegateFilepath)
      throws IOException {
    File tarFile = File.createTempFile(DELEGATE_DIR, ".tar");
    uncompressGzipFile(gzipFile, tarFile);
    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
      assertThat(tarArchiveInputStream.getNextEntry().getName()).isEqualTo(DELEGATE_DIR + "/");

      TarArchiveEntry file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/start.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).isEqualTo(0755);

      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream(expectedStartFilepath)))
                         .replaceAll("8888", "" + port));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/delegate.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).isEqualTo(0755);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream(expectedDelegateFilepath)))
                  .replaceAll("8888", "" + port));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/stop.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).isEqualTo(0755);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedStopOpenJdk.sh")))
                  .replaceAll("8888", "" + port));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/setup-proxy.sh");
      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedSetupProxy.sh"))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/README.txt");
    }
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void shouldDownloadScriptsForOpenJdk() throws IOException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    when(delegateProfileService.fetchCgPrimaryProfile(ACCOUNT_ID))
        .thenReturn(createDelegateProfileBuilder().uuid(DELEGATE_PROFILE_ID).build());
    File gzipFile = delegateService.downloadScripts(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, DELEGATE_NAME, DELEGATE_PROFILE_ID);
    File tarFile = File.createTempFile(DELEGATE_DIR, ".tar");
    uncompressGzipFile(gzipFile, tarFile);
    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
      assertThat(tarArchiveInputStream.getNextEntry().getName()).isEqualTo(DELEGATE_DIR + "/");

      TarArchiveEntry file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/start.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).isEqualTo(0755);

      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedStartOpenJdk.sh")))
                  .replaceAll("8888", "" + port));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/delegate.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).isEqualTo(0755);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedDelegateOpenJdk.sh")))
                  .replaceAll("8888", "" + port));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/stop.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).isEqualTo(0755);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedStopOpenJdk.sh")))
                  .replaceAll("8888", "" + port));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/setup-proxy.sh");
      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedSetupProxy.sh"))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/README.txt");
    }
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldDownloadDocker() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    when(delegateProfileService.get(ACCOUNT_ID, DELEGATE_PROFILE_ID))
        .thenReturn(createDelegateProfileBuilder().build());
    File gzipFile = delegateService.downloadDocker(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, DELEGATE_NAME, DELEGATE_PROFILE_ID);
    verifyDownloadDockerResult(gzipFile, "/expectedLaunchHarnessDelegate.sh");
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldDownloadDockerWithoutDelegateName() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    when(delegateProfileService.get(ACCOUNT_ID, DELEGATE_PROFILE_ID))
        .thenReturn(createDelegateProfileBuilder().build());
    File gzipFile = delegateService.downloadDocker(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, null, DELEGATE_PROFILE_ID);
    verifyDownloadDockerResult(gzipFile, "/expectedLaunchHarnessDelegateWithoutName.sh");

    gzipFile = delegateService.downloadDocker(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, "", DELEGATE_PROFILE_ID);
    verifyDownloadDockerResult(gzipFile, "/expectedLaunchHarnessDelegateWithoutName.sh");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldDownloadDockerWithPrimaryProfile() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    when(delegateProfileService.fetchCgPrimaryProfile(ACCOUNT_ID))
        .thenReturn(createDelegateProfileBuilder().uuid(DELEGATE_PROFILE_ID).build());
    File gzipFile = delegateService.downloadDocker(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, DELEGATE_NAME, null);
    verifyDownloadDockerResult(gzipFile, "/expectedLaunchHarnessDelegate.sh");

    when(delegateProfileService.get(ACCOUNT_ID, "invalidProfile")).thenReturn(null);
    when(delegateProfileService.fetchCgPrimaryProfile(ACCOUNT_ID))
        .thenReturn(createDelegateProfileBuilder().uuid(DELEGATE_PROFILE_ID).build());
    gzipFile = delegateService.downloadDocker(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, DELEGATE_NAME, "invalidProfile");
    verifyDownloadDockerResult(gzipFile, "/expectedLaunchHarnessDelegate.sh");
  }

  private void verifyDownloadDockerResult(File gzipFile, String expectedLaunchDelegateFilepath) throws IOException {
    File tarFile = File.createTempFile(DELEGATE_DIR, ".tar");
    uncompressGzipFile(gzipFile, tarFile);
    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
      assertThat(tarArchiveInputStream.getNextEntry().getName()).isEqualTo(DOCKER_DELEGATE + "/");

      TarArchiveEntry file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(ArchiveEntry::getName).isEqualTo(DOCKER_DELEGATE + "/launch-harness-delegate.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).isEqualTo(0755);

      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams
                  .toString(new InputStreamReader(getClass().getResourceAsStream(expectedLaunchDelegateFilepath)))
                  .replaceAll("8888", "" + port));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(ArchiveEntry::getName).isEqualTo(DOCKER_DELEGATE + "/README.txt");
    }
  }

  private static void uncompressGzipFile(File gzipFile, File file) throws IOException {
    try (FileInputStream fis = new FileInputStream(gzipFile); FileOutputStream fos = new FileOutputStream(file);
         GZIPInputStream gzipIS = new GZIPInputStream(fis)) {
      byte[] buffer = new byte[1024];
      int len;
      while ((len = gzipIS.read(buffer)) != -1) {
        fos.write(buffer, 0, len);
      }
    }
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldDownloadKubernetes() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    File gzipFile = delegateService.downloadKubernetes(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, "harness-delegate", "");
    File tarFile = File.createTempFile(DELEGATE_DIR, ".tar");
    uncompressGzipFile(gzipFile, tarFile);
    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
      assertThat(tarArchiveInputStream.getNextEntry().getName()).isEqualTo(KUBERNETES_DELEGATE + "/");

      TarArchiveEntry file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(ArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/harness-delegate.yaml");
      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams
                  .toString(new InputStreamReader(getClass().getResourceAsStream("/expectedHarnessDelegate.yaml")))
                  .replaceAll("8888", "" + port));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/README.txt");
    }
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldDownloadKubernetesWithCiEnabled() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    featureTestHelper.enableFeatureFlag(FeatureName.NEXT_GEN_ENABLED);
    File gzipFile = delegateService.downloadKubernetes(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, "harness-delegate", "");
    File tarFile = File.createTempFile(DELEGATE_DIR, ".tar");
    uncompressGzipFile(gzipFile, tarFile);
    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
      assertThat(tarArchiveInputStream.getNextEntry().getName()).isEqualTo(KUBERNETES_DELEGATE + "/");

      TarArchiveEntry file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(ArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/harness-delegate.yaml");
      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams
                         .toString(new InputStreamReader(
                             getClass().getResourceAsStream("/expectedHarnessDelegateWithCiEnabled.yaml")))
                         .replaceAll("8888", "" + port));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/README.txt");
    }
    featureTestHelper.disableFeatureFlag(FeatureName.NEXT_GEN_ENABLED);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldSignalForDelegateUpgradeWhenUpdateIsPresent() throws IOException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    Delegate delegate = createDelegateBuilder().build();
    delegate.setUuid(DELEGATE_ID);
    persistence.save(delegate);
    DelegateScripts delegateScripts =
        delegateService.getDelegateScripts(ACCOUNT_ID, "0.0.0", "https://localhost:9090", "https://localhost:7070");
    assertThat(delegateScripts.isDoUpgrade()).isTrue();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotSignalForDelegateUpgradeWhenDelegateIsLatest() throws IOException, TemplateException {
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.AWS);
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    Delegate delegate = createDelegateBuilder().build();
    delegate.setUuid(DELEGATE_ID);
    persistence.save(delegate);
    DelegateScripts delegateScripts =
        delegateService.getDelegateScripts(ACCOUNT_ID, "9.9.9", "https://localhost:9090", "https://localhost:7070");
    assertThat(delegateScripts.isDoUpgrade()).isFalse();
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
        delegateService.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid());
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
        delegateService.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid());
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
    assertThat(delegateService.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid())).isNotNull();
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
    assertThat(delegateService.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid())).isNull();
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
    assertThat(delegateService.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid())).isNull();
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
    assertThat(delegateService.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID + "1", delegateTask.getUuid())).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testValidateKubernetesYamlWithoutConfigurationId() {
    String accountId = generateUuid();
    DelegateSetupDetails setupDetails = DelegateSetupDetails.builder().build();

    assertThatThrownBy(() -> delegateService.validateKubernetesYaml(accountId, setupDetails))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Delegate Configuration must be provided.");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testValidateKubernetesYamlWithoutName() {
    String accountId = generateUuid();
    DelegateSetupDetails setupDetails = DelegateSetupDetails.builder().delegateConfigurationId("delConfigId").build();

    assertThatThrownBy(() -> delegateService.validateKubernetesYaml(accountId, setupDetails))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Delegate Name must be provided.");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testValidateKubernetesYamlWithoutSize() {
    String accountId = generateUuid();
    DelegateSetupDetails setupDetails =
        DelegateSetupDetails.builder().delegateConfigurationId("delConfigId").name("name").build();

    assertThatThrownBy(() -> delegateService.validateKubernetesYaml(accountId, setupDetails))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Delegate Size must be provided.");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testValidateKubernetesYamlWithoutOrWithInvalidK8sConfig() {
    String accountId = generateUuid();

    // K8sConfig is null
    DelegateSetupDetails setupDetails = DelegateSetupDetails.builder()
                                            .delegateConfigurationId("delConfigId")
                                            .name("name")
                                            .size(DelegateSize.LARGE)
                                            .description("desc")
                                            .build();

    assertThatThrownBy(() -> delegateService.validateKubernetesYaml(accountId, setupDetails))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("K8s permission type must be provided.");

    // K8sConfig does not have permission set
    DelegateSetupDetails setupDetails2 = DelegateSetupDetails.builder()
                                             .delegateConfigurationId("delConfigId")
                                             .name("name")
                                             .size(DelegateSize.LARGE)
                                             .description("desc")
                                             .k8sConfigDetails(K8sConfigDetails.builder().build())
                                             .build();

    assertThatThrownBy(() -> delegateService.validateKubernetesYaml(accountId, setupDetails2))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("K8s permission type must be provided.");

    // K8sConfig does not have namespace set for namespace admin permission
    DelegateSetupDetails setupDetails3 =
        DelegateSetupDetails.builder()
            .delegateConfigurationId("delConfigId")
            .name("name")
            .size(DelegateSize.LARGE)
            .description("desc")
            .k8sConfigDetails(K8sConfigDetails.builder().k8sPermissionType(K8sPermissionType.NAMESPACE_ADMIN).build())
            .build();

    assertThatThrownBy(() -> delegateService.validateKubernetesYaml(accountId, setupDetails3))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("K8s namespace must be provided for this type of permission.");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testValidateKubernetesYamlShouldGenerateSessionId() {
    String accountId = generateUuid();
    DelegateSetupDetails setupDetails =
        DelegateSetupDetails.builder()
            .delegateConfigurationId("delConfigId")
            .name("name")
            .size(DelegateSize.LARGE)
            .description("desc")
            .k8sConfigDetails(K8sConfigDetails.builder().k8sPermissionType(K8sPermissionType.CLUSTER_ADMIN).build())
            .build();

    DelegateSetupDetails validatedSetupDetails = delegateService.validateKubernetesYaml(accountId, setupDetails);
    assertThat(validatedSetupDetails).isEqualToIgnoringGivenFields(setupDetails, "sessionIdentifier");
    assertThat(validatedSetupDetails.getSessionIdentifier()).isNotBlank();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldGenerateKubernetesClusterAdminYaml() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    DelegateSetupDetails setupDetails =
        DelegateSetupDetails.builder()
            .sessionIdentifier("9S5HMP0xROugl3_QgO62rQ")
            .orgIdentifier("9S5HMP0xROugl3_QgO62rQO")
            .projectIdentifier("9S5HMP0xROugl3_QgO62rQP")
            .delegateConfigurationId("delConfigId")
            .name("harness-delegate")
            .size(DelegateSize.LARGE)
            .description("desc")
            .k8sConfigDetails(K8sConfigDetails.builder().k8sPermissionType(K8sPermissionType.CLUSTER_ADMIN).build())
            .build();

    persistence.save(
        DelegateGroup.builder().accountId(ACCOUNT_ID).name("harness-delegate").uuid("delegateGroupId1").build());

    File gzipFile = delegateService.generateKubernetesYaml(ACCOUNT_ID, setupDetails, "https://localhost:9090",
        "https://localhost:7070", MediaType.MULTIPART_FORM_DATA_TYPE);

    File tarFile = File.createTempFile(DELEGATE_DIR, ".tar");
    uncompressGzipFile(gzipFile, tarFile);
    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
      assertThat(tarArchiveInputStream.getNextEntry().getName()).isEqualTo(KUBERNETES_DELEGATE + "/");

      TarArchiveEntry file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(ArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/harness-delegate.yaml");
      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams
                         .toString(new InputStreamReader(
                             getClass().getResourceAsStream("/expectedHarnessDelegateNgClusterAdmin.yaml")))
                         .replaceAll("8888", "" + port));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/README.txt");
    }
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldGenerateKubernetesClusterViewerYaml() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    DelegateSetupDetails setupDetails =
        DelegateSetupDetails.builder()
            .sessionIdentifier("9S5HMP0xROugl3_QgO62rQ")
            .orgIdentifier("9S5HMP0xROugl3_QgO62rQO")
            .projectIdentifier("9S5HMP0xROugl3_QgO62rQP")
            .delegateConfigurationId("delConfigId")
            .name("harness-delegate")
            .size(DelegateSize.LARGE)
            .description("desc")
            .k8sConfigDetails(K8sConfigDetails.builder().k8sPermissionType(K8sPermissionType.CLUSTER_VIEWER).build())
            .build();

    persistence.save(
        DelegateGroup.builder().accountId(ACCOUNT_ID).name("harness-delegate").uuid("delegateGroupId1").build());

    File gzipFile = delegateService.generateKubernetesYaml(ACCOUNT_ID, setupDetails, "https://localhost:9090",
        "https://localhost:7070", MediaType.MULTIPART_FORM_DATA_TYPE);

    File tarFile = File.createTempFile(DELEGATE_DIR, ".tar");
    uncompressGzipFile(gzipFile, tarFile);
    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
      assertThat(tarArchiveInputStream.getNextEntry().getName()).isEqualTo(KUBERNETES_DELEGATE + "/");

      TarArchiveEntry file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(ArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/harness-delegate.yaml");
      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams
                         .toString(new InputStreamReader(
                             getClass().getResourceAsStream("/expectedHarnessDelegateNgClusterViewer.yaml")))
                         .replaceAll("8888", "" + port));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/README.txt");
    }
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldGenerateKubernetesNamespaceAdminYaml() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    DelegateSetupDetails setupDetails = DelegateSetupDetails.builder()
                                            .sessionIdentifier("9S5HMP0xROugl3_QgO62rQ")
                                            .orgIdentifier("9S5HMP0xROugl3_QgO62rQO")
                                            .projectIdentifier("9S5HMP0xROugl3_QgO62rQP")
                                            .delegateConfigurationId("delConfigId")
                                            .name("harness-delegate")
                                            .size(DelegateSize.LARGE)
                                            .description("desc")
                                            .k8sConfigDetails(K8sConfigDetails.builder()
                                                                  .k8sPermissionType(K8sPermissionType.NAMESPACE_ADMIN)
                                                                  .namespace("test-namespace")
                                                                  .build())
                                            .build();

    persistence.save(
        DelegateGroup.builder().accountId(ACCOUNT_ID).name("harness-delegate").uuid("delegateGroupId1").build());

    File gzipFile = delegateService.generateKubernetesYaml(ACCOUNT_ID, setupDetails, "https://localhost:9090",
        "https://localhost:7070", MediaType.MULTIPART_FORM_DATA_TYPE);

    File tarFile = File.createTempFile(DELEGATE_DIR, ".tar");
    uncompressGzipFile(gzipFile, tarFile);
    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
      assertThat(tarArchiveInputStream.getNextEntry().getName()).isEqualTo(KUBERNETES_DELEGATE + "/");

      TarArchiveEntry file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(ArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/harness-delegate.yaml");
      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams
                         .toString(new InputStreamReader(
                             getClass().getResourceAsStream("/expectedHarnessDelegateNgNamespaceAdmin.yaml")))
                         .replaceAll("8888", "" + port));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/README.txt");
    }
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldGenerateKubernetesYamlWithoutDescProjectAndOrg() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    DelegateSetupDetails setupDetails =
        DelegateSetupDetails.builder()
            .sessionIdentifier("9S5HMP0xROugl3_QgO62rQ")
            .delegateConfigurationId("delConfigId")
            .name("harness-delegate")
            .size(DelegateSize.LARGE)
            .k8sConfigDetails(K8sConfigDetails.builder().k8sPermissionType(K8sPermissionType.CLUSTER_ADMIN).build())
            .build();

    persistence.save(
        DelegateGroup.builder().accountId(ACCOUNT_ID).name("harness-delegate").uuid("delegateGroupId1").build());

    File gzipFile = delegateService.generateKubernetesYaml(ACCOUNT_ID, setupDetails, "https://localhost:9090",
        "https://localhost:7070", MediaType.MULTIPART_FORM_DATA_TYPE);

    File tarFile = File.createTempFile(DELEGATE_DIR, ".tar");
    uncompressGzipFile(gzipFile, tarFile);
    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
      assertThat(tarArchiveInputStream.getNextEntry().getName()).isEqualTo(KUBERNETES_DELEGATE + "/");

      TarArchiveEntry file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(ArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/harness-delegate.yaml");
      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams
                         .toString(new InputStreamReader(
                             getClass().getResourceAsStream("/expectedHarnessDelegateNgWithoutDescription.yaml")))
                         .replaceAll("8888", "" + port));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/README.txt");
    }
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
        delegateService.acquireDelegateTask(accountId, delegateId, generateUuid());
    assertThat(delegateTaskPackage).isNull();

    delegate.setStatus(DelegateInstanceStatus.DELETED);
    persistence.save(delegate);

    delegateTaskPackage = delegateService.acquireDelegateTask(accountId, delegateId, generateUuid());
    assertThat(delegateTaskPackage).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldNotAcquireTaskIfDelegateNotFoundInDb() {
    DelegateTaskPackage delegateTaskPackage =
        delegateService.acquireDelegateTask(ACCOUNT_ID, generateUuid(), generateUuid());
    assertThat(delegateTaskPackage).isNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFilterTaskForAccount() {
    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(ACCOUNT_ID + "1");
    delegate.setUuid(DELEGATE_ID);
    persistence.save(delegate);
    assertThat(delegateService.filter(ACCOUNT_ID, DELEGATE_ID)).isFalse();
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
    assertThat(delegateService.filter(DELEGATE_ID,
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
  public void shouldNotFilterTaskWhenItMatchesDelegateCriteria() {
    Delegate delegate = createDelegateBuilder().build();
    delegate.setUuid(DELEGATE_ID);
    persistence.save(delegate);
    assertThat(delegateService.filter(delegate.getAccountId(), DELEGATE_ID)).isTrue();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetLatestVersion() {
    assertThat(delegateService.getLatestDelegateVersion(ACCOUNT_ID)).isEqualTo("9.9.9");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testProcessDelegateTaskResponseWithDelegateMetaInfo() {
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);

    DelegateMetaInfo delegateMetaInfo = DelegateMetaInfo.builder().id(DELEGATE_ID).hostName(HOST_NAME).build();
    JenkinsExecutionResponse jenkinsExecutionResponse =
        JenkinsExecutionResponse.builder().delegateMetaInfo(delegateMetaInfo).build();

    delegateTaskService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder().accountId(ACCOUNT_ID).response(jenkinsExecutionResponse).build());
    DelegateTaskNotifyResponseData delegateTaskNotifyResponseData = jenkinsExecutionResponse;
    assertThat(delegateTaskNotifyResponseData.getDelegateMetaInfo().getHostName()).isEqualTo(HOST_NAME);
    assertThat(delegateTaskNotifyResponseData.getDelegateMetaInfo().getId()).isEqualTo(DELEGATE_ID);

    jenkinsExecutionResponse = JenkinsExecutionResponse.builder().delegateMetaInfo(delegateMetaInfo).build();
    delegateTaskNotifyResponseData = jenkinsExecutionResponse;
    persistence.save(delegateTask);
    delegateTaskService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder().accountId(ACCOUNT_ID).response(jenkinsExecutionResponse).build());
    assertThat(delegateTaskNotifyResponseData.getDelegateMetaInfo().getId()).isEqualTo(DELEGATE_ID);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldNotCheckForProfileIfManagerNotPrimary() {
    when(configurationController.isNotPrimary()).thenReturn(Boolean.TRUE);
    DelegateProfileParams delegateProfileParams = delegateService.checkForProfile(ACCOUNT_ID, DELEGATE_ID, "", 0);

    assertThat(delegateProfileParams).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldNotCheckForProfileIfNotEnabled() {
    when(configurationController.isNotPrimary()).thenReturn(Boolean.FALSE);
    String accountId = generateUuid();

    Delegate deletedDelegate = createDelegateBuilder().build();
    deletedDelegate.setStatus(DelegateInstanceStatus.DELETED);
    deletedDelegate.setAccountId(accountId);
    deletedDelegate.setUuid(generateUuid());
    persistence.save(deletedDelegate);

    DelegateProfileParams delegateProfileParams =
        delegateService.checkForProfile(accountId, deletedDelegate.getUuid(), "", 0);
    assertThat(delegateProfileParams).isNull();

    Delegate wapprDelegate = createDelegateBuilder().build();
    wapprDelegate.setStatus(DelegateInstanceStatus.WAITING_FOR_APPROVAL);
    wapprDelegate.setAccountId(accountId);
    wapprDelegate.setUuid(generateUuid());
    persistence.save(wapprDelegate);

    delegateProfileParams = delegateService.checkForProfile(accountId, wapprDelegate.getUuid(), "", 0);

    assertThat(delegateProfileParams).isNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldCheckForProfile() {
    when(configurationController.isNotPrimary()).thenReturn(Boolean.FALSE);
    Delegate delegate =
        Delegate.builder().uuid(DELEGATE_ID).accountId(ACCOUNT_ID).delegateProfileId("profile1").build();
    persistence.save(delegate);
    DelegateProfile profile = builder().accountId(ACCOUNT_ID).name("A Profile").startupScript("rm -rf /*").build();
    profile.setUuid("profile1");
    profile.setLastUpdatedAt(100L);
    when(delegateProfileService.get(ACCOUNT_ID, "profile1")).thenReturn(profile);

    DelegateProfileParams init = delegateService.checkForProfile(ACCOUNT_ID, DELEGATE_ID, "", 0);
    assertThat(init).isNotNull();
    assertThat(init.getProfileId()).isEqualTo("profile1");
    assertThat(init.getName()).isEqualTo("A Profile");
    assertThat(init.getProfileLastUpdatedAt()).isEqualTo(100L);
    assertThat(init.getScriptContent()).isEqualTo("rm -rf /*");

    init = delegateService.checkForProfile(ACCOUNT_ID, DELEGATE_ID, "profile2", 200L);
    assertThat(init).isNotNull();
    assertThat(init.getProfileId()).isEqualTo("profile1");
    assertThat(init.getName()).isEqualTo("A Profile");
    assertThat(init.getProfileLastUpdatedAt()).isEqualTo(100L);
    assertThat(init.getScriptContent()).isEqualTo("rm -rf /*");

    init = delegateService.checkForProfile(ACCOUNT_ID, DELEGATE_ID, "profile1", 99L);
    assertThat(init).isNotNull();
    assertThat(init.getProfileId()).isEqualTo("profile1");
    assertThat(init.getName()).isEqualTo("A Profile");
    assertThat(init.getProfileLastUpdatedAt()).isEqualTo(100L);
    assertThat(init.getScriptContent()).isEqualTo("rm -rf /*");

    init = delegateService.checkForProfile(ACCOUNT_ID, DELEGATE_ID, "profile1", 100L);
    assertThat(init).isNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldCheckForProfileWithSecrets() {
    EncryptedData encryptedData = EncryptedData.builder().build();
    encryptedData.setUuid(generateUuid());
    List<EncryptedDataDetail> encryptionDetails = ImmutableList.of(
        EncryptedDataDetail.builder().encryptedData(SecretManager.buildRecordData(encryptedData)).build());
    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .accountId(ACCOUNT_ID)
                                          .type(ENCRYPTED_TEXT)
                                          .encryptedValue(encryptedData.getUuid())
                                          .secretTextName("My Secret")
                                          .build();
    when(secretManager.getSecretMappedToAccountByName(ACCOUNT_ID, "My Secret")).thenReturn(encryptedData);
    when(secretManager.getEncryptionDetails(eq(serviceVariable), eq(null), eq(null))).thenReturn(encryptionDetails);

    Delegate delegate =
        Delegate.builder().uuid(DELEGATE_ID).accountId(ACCOUNT_ID).delegateProfileId("profileSecret").build();
    persistence.save(delegate);
    DelegateProfile profile = builder()
                                  .accountId(ACCOUNT_ID)
                                  .name("A Secret Profile")
                                  .startupScript("A secret: ${secrets.getValue(\"My Secret\")}")
                                  .build();
    profile.setUuid("profileSecret");
    profile.setLastUpdatedAt(100L);
    when(delegateProfileService.get(ACCOUNT_ID, "profileSecret")).thenReturn(profile);

    doAnswer(invocation -> {
      ((ServiceVariable) invocation.getArguments()[0]).setValue("Shhh! This is a secret!".toCharArray());
      return null;
    })
        .when(managerDecryptionService)
        .decrypt(eq(serviceVariable), eq(encryptionDetails));

    DelegateProfileParams init = delegateService.checkForProfile(ACCOUNT_ID, DELEGATE_ID, "", 0);

    assertThat(init).isNotNull();
    assertThat(init.getProfileId()).isEqualTo("profileSecret");
    assertThat(init.getName()).isEqualTo("A Secret Profile");
    assertThat(init.getProfileLastUpdatedAt()).isEqualTo(100L);
    assertThat(init.getScriptContent()).isEqualTo("A secret: Shhh! This is a secret!");
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldSaveProfileResult_NoPrevious() {
    Delegate previousDelegate =
        Delegate.builder().uuid(DELEGATE_ID).accountId(ACCOUNT_ID).hostName("hostname").ip("1.2.3.4").build();
    persistence.save(previousDelegate);

    String content = "This is the profile result text";
    FormDataContentDisposition fileDetail =
        FormDataContentDisposition.name("profile-result").fileName("profile.result").build();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(UTF_8));

    when(fileService.saveFile(any(FileMetadata.class), any(InputStream.class), eq(FileBucket.PROFILE_RESULTS)))
        .thenReturn("file_id");

    long now = System.currentTimeMillis();
    delegateService.saveProfileResult(
        ACCOUNT_ID, DELEGATE_ID, false, FileBucket.PROFILE_RESULTS, inputStream, fileDetail);

    verify(alertService)
        .closeAlert(eq(ACCOUNT_ID), eq(GLOBAL_APP_ID), eq(AlertType.DelegateProfileError),
            eq(DelegateProfileErrorAlert.builder()
                    .accountId(ACCOUNT_ID)
                    .hostName("hostname")
                    .obfuscatedIpAddress(obfuscate("1.2.3.4"))
                    .build()));

    Delegate delegate = persistence.get(Delegate.class, DELEGATE_ID);
    assertThat(delegate.getProfileExecutedAt()).isGreaterThanOrEqualTo(now);
    assertThat(delegate.isProfileError()).isFalse();
    assertThat(delegate.getProfileResult()).isEqualTo("file_id");
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldSaveProfileResult_WithPrevious() {
    Delegate previousDelegate =
        Delegate.builder().uuid(DELEGATE_ID).accountId(ACCOUNT_ID).hostName("hostname").ip("1.2.3.4").build();
    previousDelegate.setProfileResult("previous-result");
    persistence.save(previousDelegate);

    String content = "This is the profile result text";
    FormDataContentDisposition fileDetail =
        FormDataContentDisposition.name("profile-result").fileName("profile.result").build();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(UTF_8));

    when(fileService.saveFile(any(FileMetadata.class), any(InputStream.class), eq(FileBucket.PROFILE_RESULTS)))
        .thenReturn("file_id");

    long now = System.currentTimeMillis();
    delegateService.saveProfileResult(
        ACCOUNT_ID, DELEGATE_ID, true, FileBucket.PROFILE_RESULTS, inputStream, fileDetail);

    verify(alertService)
        .openAlert(eq(ACCOUNT_ID), eq(GLOBAL_APP_ID), eq(AlertType.DelegateProfileError),
            eq(DelegateProfileErrorAlert.builder()
                    .accountId(ACCOUNT_ID)
                    .hostName("hostname")
                    .obfuscatedIpAddress(obfuscate("1.2.3.4"))
                    .build()));

    verify(fileService).deleteFile(eq("previous-result"), eq(FileBucket.PROFILE_RESULTS));

    Delegate delegate = persistence.get(Delegate.class, DELEGATE_ID);
    assertThat(delegate.getProfileExecutedAt()).isGreaterThanOrEqualTo(now);
    assertThat(delegate.isProfileError()).isTrue();
    assertThat(delegate.getProfileResult()).isEqualTo("file_id");
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetProfileResult() {
    Delegate delegate =
        Delegate.builder().uuid(DELEGATE_ID).accountId(ACCOUNT_ID).hostName("hostname").ip("1.2.3.4").build();
    delegate.setProfileResult("result_file_id");
    persistence.save(delegate);

    String content = "This is the profile result text";

    doAnswer(invocation -> {
      ((OutputStream) invocation.getArguments()[1]).write(content.getBytes(UTF_8));
      return null;
    })
        .when(fileService)
        .downloadToStream(eq("result_file_id"), any(OutputStream.class), eq(FileBucket.PROFILE_RESULTS));

    String result = delegateService.getProfileResult(ACCOUNT_ID, DELEGATE_ID);
    assertThat(result).isEqualTo(content);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetKubernetesDelegateNames() {
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .hostName("a.b.c")
                            .version(VERSION)
                            .status(DelegateInstanceStatus.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .build();
    persistence.save(delegate);
    delegate = Delegate.builder()
                   .accountId(ACCOUNT_ID)
                   .ip("127.0.0.1")
                   .hostName("d.e.f")
                   .delegateName("k8s-name")
                   .version(VERSION)
                   .status(DelegateInstanceStatus.ENABLED)
                   .lastHeartBeat(System.currentTimeMillis())
                   .build();
    persistence.save(delegate);
    List<String> k8sNames = delegateService.getKubernetesDelegateNames(ACCOUNT_ID);
    assertThat(k8sNames.size()).isEqualTo(1);
    assertThat(k8sNames.get(0)).isEqualTo("k8s-name");
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetAllDelegateSelectors() {
    DelegateProfile delegateProfile =
        DelegateProfile.builder().uuid(generateUuid()).accountId(ACCOUNT_ID).name(generateUuid()).build();
    persistence.save(delegateProfile);

    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .hostName("a.b.c")
                            .delegateName("testDelegateName1")
                            .version(VERSION)
                            .status(DelegateInstanceStatus.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .delegateProfileId(delegateProfile.getUuid())
                            .tags(ImmutableList.of("abc"))
                            .build();
    persistence.save(delegate);
    delegate = Delegate.builder()
                   .accountId(ACCOUNT_ID)
                   .ip("127.0.0.1")
                   .hostName("d.e.f")
                   .delegateName("testDelegateName2")
                   .version(VERSION)
                   .status(DelegateInstanceStatus.ENABLED)
                   .lastHeartBeat(System.currentTimeMillis())
                   .delegateProfileId(delegateProfile.getUuid())
                   .tags(ImmutableList.of("def"))
                   .build();

    persistence.save(delegate);

    Set<String> tags = delegateService.getAllDelegateSelectors(ACCOUNT_ID);
    assertThat(tags.size()).isEqualTo(7);
    assertThat(tags).containsExactlyInAnyOrder("abc", "def", "testdelegatename1", "testdelegatename2", "a.b.c", "d.e.f",
        delegateProfile.getName().toLowerCase());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldGetAllDelegateSelectorsEmptySelectors() {
    DelegateProfile delegateProfile =
        DelegateProfile.builder().uuid(generateUuid()).accountId(ACCOUNT_ID).name(PRIMARY_PROFILE_NAME).build();
    persistence.save(delegateProfile);

    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .hostName("a.b.c")
                            .delegateName("testDelegateName1")
                            .version(VERSION)
                            .status(DelegateInstanceStatus.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .delegateProfileId(delegateProfile.getUuid())
                            .build();
    persistence.save(delegate);

    Set<String> tags = delegateService.getAllDelegateSelectors(ACCOUNT_ID);
    assertThat(tags.size()).isEqualTo(3);
    assertThat(tags).containsExactlyInAnyOrder("testdelegatename1", "a.b.c", PRIMARY_PROFILE_NAME);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldRetrieveDelegateSelectors() {
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .version(VERSION)
                            .status(DelegateInstanceStatus.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .tags(ImmutableList.of("abc", "qwe", "xde"))
                            .build();
    persistence.save(delegate);

    Set<String> tags = delegateService.retrieveDelegateSelectors(delegate);
    assertThat(tags.size()).isEqualTo(3);
    assertThat(tags).containsExactlyInAnyOrder("abc", "qwe", "xde");
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldRetrieveDelegateSelectors_Empty() {
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .version(VERSION)
                            .status(DelegateInstanceStatus.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .build();
    persistence.save(delegate);

    Set<String> tags = delegateService.retrieveDelegateSelectors(delegate);
    assertThat(tags.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetAllDelegateSelectors_Empty() {
    Set<String> tags = delegateService.getAllDelegateSelectors(ACCOUNT_ID);
    assertThat(tags.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetAvailableVersions() {
    List<String> versions = delegateService.getAvailableVersions(ACCOUNT_ID);
    assertThat(versions.size()).isEqualTo(1);
    assertThat(versions).containsExactly("0.0.0");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testDelegateDisconnected() {
    String delegateConnectionId = generateUuid();
    delegateService.registerHeartbeat(ACCOUNT_ID, DELEGATE_ID,
        DelegateConnectionHeartbeat.builder()
            .delegateConnectionId(delegateConnectionId)
            .version(versionInfoManager.getVersionInfo().getVersion())
            .build(),
        ConnectionMode.POLLING);

    assertThat(delegateService.checkDelegateConnected(ACCOUNT_ID, DELEGATE_ID)).isTrue();

    delegateConnectionDao.delegateDisconnected(ACCOUNT_ID, delegateConnectionId);

    assertThat(delegateService.checkDelegateConnected(ACCOUNT_ID, DELEGATE_ID)).isFalse();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldReportConnectionResults_success() {
    DelegateTask delegateTask = saveDelegateTask(false, emptySet(), QUEUED);
    DelegateTaskPackage delegateTaskPackage =
        delegateService.reportConnectionResults(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
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
        delegateService.reportConnectionResults(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
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
        delegateService.reportConnectionResults(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
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
    delegateService.failIfAllDelegatesFailed(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid());
    verify(assignDelegateService).connectedWhitelistedDelegates(delegateTask);
    assertThat(persistence.createQuery(DelegateTask.class).get()).isNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFailIfAllDelegatesFailed_sync() {
    DelegateTask delegateTask = saveDelegateTask(false, ImmutableSet.of(DELEGATE_ID), QUEUED);
    when(assignDelegateService.connectedWhitelistedDelegates(delegateTask)).thenReturn(emptyList());
    delegateService.failIfAllDelegatesFailed(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid());
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
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFailIfAllDelegatesFailed_notAll() {
    DelegateTask delegateTask = saveDelegateTask(true, ImmutableSet.of(DELEGATE_ID, "delegate2"), QUEUED);
    delegateService.failIfAllDelegatesFailed(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid());
    assertThat(persistence.createQuery(DelegateTask.class).get()).isEqualTo(delegateTask);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFailIfAllDelegatesFailed_whitelist() {
    DelegateTask delegateTask = saveDelegateTask(true, ImmutableSet.of(DELEGATE_ID), QUEUED);
    when(assignDelegateService.connectedWhitelistedDelegates(delegateTask)).thenReturn(singletonList("delegate2"));
    delegateService.failIfAllDelegatesFailed(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid());
    verify(assignDelegateService).connectedWhitelistedDelegates(delegateTask);
    assertThat(persistence.createQuery(DelegateTask.class).get()).isEqualTo(delegateTask);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldExpireTask() {
    DelegateTask delegateTask = saveDelegateTask(true, ImmutableSet.of(DELEGATE_ID), QUEUED);
    delegateService.expireTask(ACCOUNT_ID, delegateTask.getUuid());
    assertThat(persistence.createQuery(DelegateTask.class).get().getStatus()).isEqualTo(ERROR);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldAbortTask() {
    DelegateTask delegateTask = saveDelegateTask(true, ImmutableSet.of(DELEGATE_ID), QUEUED);

    DelegateTask oldTask = delegateService.abortTask(ACCOUNT_ID, delegateTask.getUuid());

    assertThat(oldTask.getUuid()).isEqualTo(delegateTask.getUuid());
    assertThat(oldTask.getStatus()).isEqualTo(QUEUED);
    assertThat(persistence.createQuery(DelegateTask.class).get().getStatus()).isEqualTo(ABORTED);
  }

  @Test
  @Owner(developers = NIKOLA)
  @Category(UnitTests.class)
  public void shouldGetCountOfDelegatesForAccounts() {
    String accountId1 = generateUuid();
    String accountId2 = generateUuid();
    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(accountId1);
    delegate.setUuid(generateUuid());

    DelegateProfile delegateProfile =
        createDelegateProfileBuilder().accountId(delegate.getAccountId()).uuid(generateUuid()).build();
    delegate.setDelegateProfileId(delegateProfile.getUuid());

    when(delegateProfileService.get(delegate.getAccountId(), delegateProfile.getUuid())).thenReturn(delegateProfile);
    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId1)).thenReturn(Integer.MAX_VALUE);

    delegate = delegateService.add(delegate);
    assertThat(persistence.get(Delegate.class, delegate.getUuid())).isEqualTo(delegate);

    List<String> accountIds = Arrays.asList(accountId1, accountId2);
    List<Integer> countOfDelegatesForAccounts = delegateService.getCountOfDelegatesForAccounts(accountIds);
    assertThat(countOfDelegatesForAccounts).hasSize(2);

    assertThat(countOfDelegatesForAccounts.get(0)).isEqualTo(1);
    assertThat(countOfDelegatesForAccounts.get(1)).isEqualTo(0);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldUpdateHeartbeatForDelegateWithPollingEnabled() {
    Delegate delegate = Delegate.builder()
                            .uuid(generateUuid())
                            .hostName(HOST_NAME)
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .version(VERSION)
                            .status(DelegateInstanceStatus.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .tags(ImmutableList.of("abc", "qwe"))
                            .build();

    persistence.save(delegate);
    when(licenseService.isAccountDeleted(anyString())).thenReturn(true);

    Delegate result = delegateService.updateHeartbeatForDelegateWithPollingEnabled(delegate);

    assertThat(result).extracting(Delegate::getStatus).isEqualTo(DelegateInstanceStatus.DELETED);
    assertThat(result).extracting(Delegate::getUuid).isEqualTo(delegate.getUuid());
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

    delegateService.scheduleSyncTask(task);
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

    delegateService.scheduleSyncTask(task);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testConvertSelectorsToExecutionCapabilityTaskSelectors_TaskSelectors() {
    List<String> selectors = Arrays.asList("a", "b");

    DelegateTask delegateTask =
        DelegateTask.builder().accountId(ACCOUNT_ID).delegateId(DELEGATE_ID).tags(selectors).build();

    delegateService.convertToExecutionCapability(delegateTask);

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

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(ACCOUNT_ID)
                                    .delegateId(DELEGATE_ID)
                                    .data(TaskData.builder().taskType(TaskType.HTTP.name()).build())
                                    .build();

    delegateService.convertToExecutionCapability(delegateTask);

    List<SelectorCapability> selectorsCapabilityList = delegateTask.getExecutionCapabilities()
                                                           .stream()
                                                           .filter(c -> c instanceof SelectorCapability)
                                                           .map(c -> (SelectorCapability) c)
                                                           .collect(Collectors.toList());

    assertThat(selectorsCapabilityList.get(0).getSelectorOrigin()).isEqualTo(TASK_CATEGORY_MAP);
    assertThat(selectorsCapabilityList.get(0).getSelectors()).isEqualTo(sampleMap.getSelectors());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testFetchAvailableSizes() {
    List<DelegateSizeDetails> delegateSizeDetails = delegateService.fetchAvailableSizes();
    assertThat(delegateSizeDetails).isNotNull();
    assertThat(delegateSizeDetails).hasSize(4);
    assertThat(delegateSizeDetails)
        .containsExactlyInAnyOrder(DelegateSizeDetails.builder()
                                       .size(DelegateSize.LAPTOP)
                                       .label("Laptop")
                                       .taskLimit(50)
                                       .replicas(1)
                                       .ram(1650)
                                       .cpu(0.5)
                                       .build(),
            DelegateSizeDetails.builder()
                .size(DelegateSize.SMALL)
                .label("Small")
                .taskLimit(100)
                .replicas(2)
                .ram(3300)
                .cpu(1)
                .build(),
            DelegateSizeDetails.builder()
                .size(DelegateSize.MEDIUM)
                .label("Medium")
                .taskLimit(200)
                .replicas(4)
                .ram(6600)
                .cpu(2)
                .build(),
            DelegateSizeDetails.builder()
                .size(DelegateSize.LARGE)
                .label("Large")
                .taskLimit(400)
                .replicas(8)
                .ram(13200)
                .cpu(4)
                .build());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testBuildCapabilitiesCheckTask() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    CapabilityCheckDetails capabilityCheckDetails1 = buildCapabilityCheckDetails(accountId, delegateId, generateUuid());
    CapabilityCheckDetails capabilityCheckDetails2 = buildCapabilityCheckDetails(accountId, delegateId, generateUuid());

    DelegateTask capabilitiesCheckTask = delegateService.buildCapabilitiesCheckTask(
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
  public void testIsDelegateInCapabilityScope() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    CapabilityTaskSelectionDetails taskSelectionDetails = buildCapabilityTaskSelectionDetails();

    when(assignDelegateService.canAssign(eq(null), eq(delegateId), eq(accountId), eq("app1"), eq("env1"), eq("infra1"),
             eq(taskSelectionDetails.getTaskGroup()), any(List.class),
             eq(taskSelectionDetails.getTaskSetupAbstractions())))
        .thenReturn(true);

    // Test with all arguments
    assertThat(delegateService.isDelegateInCapabilityScope(accountId, delegateId, taskSelectionDetails)).isTrue();

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(assignDelegateService)
        .canAssign(eq(null), eq(delegateId), eq(accountId), eq("app1"), eq("env1"), eq("infra1"),
            eq(taskSelectionDetails.getTaskGroup()), captor.capture(),
            eq(taskSelectionDetails.getTaskSetupAbstractions()));

    List<ExecutionCapability> selectorCapabilities = captor.getValue();
    assertThat(selectorCapabilities).hasSize(2);

    // Test with partial arguments
    taskSelectionDetails.setTaskSelectors(null);
    taskSelectionDetails.setTaskSetupAbstractions(null);
    taskSelectionDetails.setTaskGroup(null);

    delegateService.isDelegateInCapabilityScope(accountId, delegateId, taskSelectionDetails);

    captor = ArgumentCaptor.forClass(List.class);
    verify(assignDelegateService)
        .canAssign(eq(null), eq(delegateId), eq(accountId), eq(null), eq(null), eq(null), eq(null), captor.capture(),
            eq(null));

    selectorCapabilities = captor.getValue();
    assertThat(selectorCapabilities).isEmpty();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testIsDelegateStillInScope() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    String capabilityId = generateUuid();

    // Test no task selection details case
    when(capabilityService.getAllCapabilityTaskSelectionDetails(accountId, capabilityId)).thenReturn(null);
    assertThat(delegateService.isDelegateStillInScope(accountId, delegateId, capabilityId)).isTrue();

    // Test delegate in scope case
    CapabilityTaskSelectionDetails taskSelectionDetails = buildCapabilityTaskSelectionDetails();
    when(capabilityService.getAllCapabilityTaskSelectionDetails(accountId, capabilityId))
        .thenReturn(Collections.singletonList(taskSelectionDetails));
    when(assignDelegateService.canAssign(any(null), eq(delegateId), eq(accountId), eq("app1"), eq("env1"), eq("infra1"),
             eq(taskSelectionDetails.getTaskGroup()), any(List.class),
             eq(taskSelectionDetails.getTaskSetupAbstractions())))
        .thenReturn(true);

    assertThat(delegateService.isDelegateStillInScope(accountId, delegateId, capabilityId)).isTrue();

    // Test delegate out of scope case
    taskSelectionDetails.setAccountId(accountId);
    taskSelectionDetails.setBlocked(false);
    persistence.save(taskSelectionDetails);

    when(assignDelegateService.canAssign(any(null), anyString(), eq(accountId), eq("app1"), eq("env1"), eq("infra1"),
             eq(taskSelectionDetails.getTaskGroup()), any(List.class),
             eq(taskSelectionDetails.getTaskSetupAbstractions())))
        .thenReturn(false);
    when(capabilityService.getNotDeniedCapabilityPermissions(accountId, capabilityId))
        .thenReturn(Collections.singletonList(
            buildCapabilitySubjectPermission(accountId, generateUuid(), capabilityId, PermissionResult.ALLOWED)));

    assertThat(delegateService.isDelegateStillInScope(accountId, delegateId, capabilityId)).isFalse();

    CapabilityTaskSelectionDetails updatedTaskSelectionDetails =
        persistence.get(CapabilityTaskSelectionDetails.class, taskSelectionDetails.getUuid());
    assertThat(updatedTaskSelectionDetails).isNotNull();
    assertThat(updatedTaskSelectionDetails.isBlocked()).isTrue();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testRegenerateCapabilityPermissions() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    CapabilityRequirement capabilityRequirement1 = buildCapabilityRequirement();
    capabilityRequirement1.setAccountId(accountId);
    CapabilityRequirement capabilityRequirement2 = buildCapabilityRequirement();
    capabilityRequirement2.setAccountId(accountId);

    when(capabilityService.getAllCapabilityRequirements(accountId))
        .thenReturn(Arrays.asList(capabilityRequirement1, capabilityRequirement2));

    CapabilityTaskSelectionDetails taskSelectionDetails = buildCapabilityTaskSelectionDetails();
    when(capabilityService.getAllCapabilityTaskSelectionDetails(accountId, capabilityRequirement2.getUuid()))
        .thenReturn(Collections.singletonList(taskSelectionDetails));
    when(assignDelegateService.canAssign(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(false);
    when(capabilityService.getNotDeniedCapabilityPermissions(accountId, capabilityRequirement2.getUuid()))
        .thenReturn(Collections.emptyList());
    when(capabilityService.getAllCapabilityPermissions(accountId, capabilityRequirement1.getUuid(), null))
        .thenReturn(Collections.emptyList());

    delegateService.regenerateCapabilityPermissions(accountId, delegateId);

    verify(capabilityService)
        .deleteCapabilitySubjectPermission(accountId, delegateId, capabilityRequirement2.getUuid());
    verify(capabilityService)
        .addCapabilityPermissions(
            eq(capabilityRequirement1), any(List.class), eq(PermissionResult.UNCHECKED), eq(true));
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testCreateCapabilityRequirementInstances() {
    String accountId = generateUuid();

    HttpConnectionExecutionCapability httpCapability1 =
        HttpConnectionExecutionCapability.builder().url("https://google.com").build();
    HttpConnectionExecutionCapability httpCapability2 =
        HttpConnectionExecutionCapability.builder().url("https://harness.io").build();

    CapabilityRequirement expectedCapabilityRequirement = buildCapabilityRequirement();
    when(capabilityService.buildCapabilityRequirement(accountId, httpCapability1))
        .thenReturn(expectedCapabilityRequirement);
    when(capabilityService.buildCapabilityRequirement(accountId, httpCapability2)).thenReturn(null);

    List<CapabilityRequirement> capabilityRequirements =
        delegateService.createCapabilityRequirementInstances(accountId, asList(httpCapability1, httpCapability2));
    assertThat(capabilityRequirements).hasSize(1);
    assertThat(capabilityRequirements.get(0)).isEqualTo(expectedCapabilityRequirement);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testCreateCapabilityTaskSelectionDetailsInstance() {
    DelegateTask task = DelegateTask.builder().build();
    CapabilityRequirement capabilityRequirement = buildCapabilityRequirement();

    // Test case with partial arguments
    delegateService.createCapabilityTaskSelectionDetailsInstance(task, capabilityRequirement, null);
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

    delegateService.createCapabilityTaskSelectionDetailsInstance(task, capabilityRequirement, assignableDelegateIds);

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

    delegateService.executeBatchCapabilityCheckTask(
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

    delegateService.executeBatchCapabilityCheckTask(
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

    delegateService.executeBatchCapabilityCheckTask(
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

    delegateService.executeBatchCapabilityCheckTask(
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

    delegateService.executeBatchCapabilityCheckTask(
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
    delegateService.upsertCapabilityRequirements(DelegateTask.builder().accountId(accountId).build());

    // Test FF enabled, but no task capabilities
    featureTestHelper.enableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
    delegateService.upsertCapabilityRequirements(DelegateTask.builder().accountId(accountId).build());

    // Test FF enabled, but no task AGENT capabilities
    delegateService.upsertCapabilityRequirements(
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

    delegateService.upsertCapabilityRequirements(task);

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
    assertThat(delegateService.pickDelegateForTaskWithoutAnyAgentCapabilities(task, null)).isNull();

    // Test assignable delegate with ignore already tried case
    BatchDelegateSelectionLog selectionLogBatch = BatchDelegateSelectionLog.builder().build();
    when(delegateSelectionLogsService.createBatch(task)).thenReturn(selectionLogBatch);
    when(assignDelegateService.canAssign(eq(selectionLogBatch), anyString(), eq(task))).thenReturn(true);

    assertThat(
        delegateService.pickDelegateForTaskWithoutAnyAgentCapabilities(task, Collections.singletonList(delegateId)))
        .isEqualTo(delegateId);
    verify(delegateSelectionLogsService).createBatch(task);
    verify(delegateSelectionLogsService).save(selectionLogBatch);

    task.setAlreadyTriedDelegates(Stream.of(delegateId).collect(Collectors.toSet()));
    assertThat(
        delegateService.pickDelegateForTaskWithoutAnyAgentCapabilities(task, Collections.singletonList(delegateId)))
        .isEqualTo(delegateId);
    verify(delegateSelectionLogsService, times(2)).createBatch(task);
    verify(delegateSelectionLogsService, times(2)).save(selectionLogBatch);

    // Test assignable delegate without ignoring already tried case
    String delegateId2 = generateUuid();
    assertThat(
        delegateService.pickDelegateForTaskWithoutAnyAgentCapabilities(task, Arrays.asList(delegateId, delegateId2)))
        .isEqualTo(delegateId2);
    verify(delegateSelectionLogsService, times(3)).createBatch(task);
    verify(delegateSelectionLogsService, times(3)).save(selectionLogBatch);

    // Test out of scope case
    when(assignDelegateService.canAssign(eq(selectionLogBatch), anyString(), eq(task))).thenReturn(false);
    assertThat(
        delegateService.pickDelegateForTaskWithoutAnyAgentCapabilities(task, Arrays.asList(delegateId, delegateId2)))
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
    assertThat(delegateService.obtainCapableDelegateId(task, null)).isEqualTo(delegateId);
    verify(delegateSelectionLogsService, never()).createBatch(task);

    featureTestHelper.enableFeatureFlag(FeatureName.PER_AGENT_CAPABILITIES);
    BatchDelegateSelectionLog selectionLogBatch = BatchDelegateSelectionLog.builder().build();
    when(delegateSelectionLogsService.createBatch(task)).thenReturn(selectionLogBatch);
    List<String> activeDelegates = Arrays.asList(delegateId, delegateId2);

    // Test no task capabilities case
    when(assignDelegateService.retrieveActiveDelegates(task.getAccountId(), selectionLogBatch)).thenReturn(null);
    assertThat(delegateService.obtainCapableDelegateId(task, null)).isNull();
    verify(delegateSelectionLogsService).createBatch(task);
    verify(delegateSelectionLogsService).save(selectionLogBatch);

    // Test no AGENT capabilities case
    task.setExecutionCapabilities(asList(SelectorCapability.builder()
                                             .selectorOrigin("TASK_SELECTORS")
                                             .selectors(Collections.singleton("sel1"))
                                             .build()));
    // HttpConnectionExecutionCapability.builder().url("https://google.com").build()))
    assertThat(delegateService.obtainCapableDelegateId(task, null)).isNull();

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

    assertThat(delegateService.obtainCapableDelegateId(task, Collections.singleton(delegateId3))).isEqualTo(delegateId);

    // Test case with no capable delegates
    when(capabilityService.getCapableDelegateIds(eq(task.getAccountId()), any())).thenReturn(Collections.emptySet());
    assertThat(delegateService.obtainCapableDelegateId(task, Collections.singleton(delegateId3))).isNull();

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

    delegateService.saveDelegateTask(delegateTask, status);
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

  private void validateDelegateInnerProperties(String delegateId, DelegateStatus.DelegateInner delegateFromStatus) {
    assertThat(delegateFromStatus.getHostName()).isEqualTo("localhost");
    assertThat(delegateFromStatus.getIp()).isEqualTo("127.0.0.1");
    assertThat(delegateFromStatus.getDelegateName()).isEqualTo("testDelegateName");
    assertThat(delegateFromStatus.getDelegateType()).isEqualTo("dockerType");
    assertThat(delegateFromStatus).hasFieldOrPropertyWithValue("uuid", delegateId);
    assertThat(delegateFromStatus.getConnections()).hasSize(1);
    assertThat(delegateFromStatus.getConnections().get(0)).hasFieldOrPropertyWithValue("version", VERSION);
  }
}
