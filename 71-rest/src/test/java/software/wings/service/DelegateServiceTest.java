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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.beans.EncryptedData;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.FileMetadata;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.configuration.DeployMode;
import io.harness.delegate.beans.ConnectionMode;
import io.harness.delegate.beans.DelegateApproval;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.delegate.beans.DelegateConnectionHeartbeat;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateParams;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfileParams;
import io.harness.delegate.beans.DelegateRegisterResponse;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskEvent;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskRank;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.DelegateTaskResponse.ResponseCode;
import io.harness.delegate.beans.DuplicateDelegateException;
import io.harness.delegate.beans.NoAvailableDelegatesException;
import io.harness.delegate.beans.NoInstalledDelegatesException;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.TaskSelectorMap;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.service.DelegateAgentFileService.FileBucket;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.WingsException;
import io.harness.logstreaming.LogStreamingServiceConfig;
import io.harness.persistence.HPersistence;
import io.harness.rule.Cache;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.Cd1SetupFields;
import io.harness.version.VersionInfo;
import io.harness.version.VersionInfoManager;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.WingsBaseTest;
import software.wings.app.DelegateGrpcConfig;
import software.wings.app.FileUploadLimit;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateBuilder;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.Delegate.Status;
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
import software.wings.dl.WingsPersistence;
import software.wings.features.api.UsageLimitedFeature;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.jre.JreConfig;
import software.wings.licensing.LicenseService;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.DelegateConnectionDao;
import software.wings.service.impl.DelegateTaskBroadcastHelper;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.impl.infra.InfraDownloadService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.DelegateService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class DelegateServiceTest extends WingsBaseTest {
  private static final String VERSION = "1.0.0";
  private static final String DELEGATE_NAME = "harness-delegate";
  private static final String DELEGATE_PROFILE_ID = "QFWin33JRlKWKBzpzE5A9A";

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

  @Inject private DelegateConnectionDao delegateConnectionDao;
  @Inject private KryoSerializer kryoSerializer;

  @Rule public WireMockRule wireMockRule = new WireMockRule(8888);
  @Rule public ExpectedException thrown = ExpectedException.none();

  @InjectMocks @Inject private DelegateTaskBroadcastHelper delegateTaskBroadcastHelper;
  @InjectMocks @Inject private DelegateService delegateService;
  @Mock private UsageLimitedFeature delegatesFeature;

  @Inject private WingsPersistence wingsPersistence;

  private Account account =
      anAccount().withLicenseInfo(LicenseInfo.builder().accountStatus(AccountStatus.ACTIVE).build()).build();

  private DelegateProfileBuilder createDelegateProfileBuilder() {
    return DelegateProfile.builder().name("DELEGATE_PROFILE_NAME").description("DELEGATE_PROFILE_DESC");
  }

  @Before
  public void setUp() {
    CdnConfig cdnConfig = new CdnConfig();
    cdnConfig.setUrl("http://localhost:9500");
    when(subdomainUrlHelper.getDelegateMetadataUrl(any(), any(), any()))
        .thenReturn("http://localhost:8888/delegateci.txt");
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES);
    when(mainConfiguration.getKubectlVersion()).thenReturn("v1.12.2");
    when(mainConfiguration.getOcVersion()).thenReturn("v4.2.16");
    when(mainConfiguration.getCdnConfig()).thenReturn(cdnConfig);
    HashMap<String, JreConfig> jreConfigMap = new HashMap<>();
    jreConfigMap.put("openjdk8u242", getOpenjdkJreConfig());
    when(mainConfiguration.getCurrentJre()).thenReturn("openjdk8u242");
    when(mainConfiguration.getJreConfigs()).thenReturn(jreConfigMap);
    when(subdomainUrlHelper.getWatcherMetadataUrl(any(), any(), any()))
        .thenReturn("http://localhost:8888/watcherci.txt");
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
        .thenReturn("http://localhost:8888/builds/9/delegate.jar");
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
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldList() {
    String accountId = generateUuid();
    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(accountId);
    wingsPersistence.save(delegate);
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
    wingsPersistence.save(delegate);
    assertThat(delegateService.get(accountId, delegate.getUuid(), true)).isEqualTo(delegate);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void shouldGetDelegateStatus() {
    String accountId = generateUuid();
    when(accountService.getDelegateConfiguration(anyString()))
        .thenReturn(DelegateConfiguration.builder().watcherVersion(VERSION).delegateVersions(asList(VERSION)).build());
    Delegate delegate = createDelegateBuilder().build();

    delegate.setAccountId(accountId);

    Delegate deletedDelegate = createDelegateBuilder().build();
    deletedDelegate.setAccountId(accountId);
    deletedDelegate.setStatus(Status.DELETED);

    wingsPersistence.save(Arrays.asList(delegate, deletedDelegate));

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
        .thenReturn(DelegateConfiguration.builder().watcherVersion(VERSION).delegateVersions(asList(VERSION)).build());

    Delegate deletedDelegate = createDelegateBuilder().build();
    deletedDelegate.setAccountId(accountId);
    deletedDelegate.setStatus(Status.DELETED);

    Delegate delegateWithoutScalingGroup = createDelegateBuilder().build();
    delegateWithoutScalingGroup.setAccountId(accountId);

    wingsPersistence.save(Arrays.asList(delegateWithoutScalingGroup, deletedDelegate));
    delegateService.registerHeartbeat(accountId, delegateWithoutScalingGroup.getUuid(),
        DelegateConnectionHeartbeat.builder().delegateConnectionId(generateUuid()).version(VERSION).build(),
        ConnectionMode.POLLING);

    DelegateStatus delegateStatus = delegateService.getDelegateStatusWithScalingGroups(accountId);

    assertThat(delegateStatus.getPublishedVersions()).hasSize(1).contains(VERSION);
    assertThat(delegateStatus.getDelegates()).hasSize(1);
    validateDelegateInnerProperties(delegateWithoutScalingGroup.getUuid(), delegateStatus.getDelegates().get(0));
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldGetDelegateStatus2ScalingGroupHasCorrectItems() {
    String accountId = generateUuid();
    when(accountService.getDelegateConfiguration(anyString()))
        .thenReturn(DelegateConfiguration.builder().watcherVersion(VERSION).delegateVersions(asList(VERSION)).build());

    Delegate deletedDelegate = createDelegateBuilder().build();
    deletedDelegate.setAccountId(accountId);
    deletedDelegate.setStatus(Status.DELETED);

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

    wingsPersistence.save(Arrays.asList(deletedDelegate, delegateWithScalingGroup1, delegateWithScalingGroup2,
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
        .thenReturn(DelegateConfiguration.builder().watcherVersion(VERSION).delegateVersions(asList(VERSION)).build());

    Delegate deletedDelegate = createDelegateBuilder().build();
    deletedDelegate.setAccountId(accountId);
    deletedDelegate.setStatus(Status.DELETED);
    deletedDelegate.setDelegateGroupName("test");
    wingsPersistence.save(deletedDelegate);

    DelegateStatus status = delegateService.getDelegateStatusWithScalingGroups(accountId);

    assertThat(status.getScalingGroups()).isNotNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldUpdate() {
    String accountId = generateUuid();
    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(accountId);
    wingsPersistence.save(delegate);
    delegate.setLastHeartBeat(System.currentTimeMillis());
    delegate.setStatus(Status.DISABLED);
    delegateService.update(delegate);
    Delegate updatedDelegate = wingsPersistence.get(Delegate.class, delegate.getUuid());
    assertThat(updatedDelegate).isEqualToIgnoringGivenFields(delegate, DelegateKeys.validUntil);
    verify(eventEmitter)
        .send(Channel.DELEGATES,
            anEvent().withOrgId(accountId).withUuid(delegate.getUuid()).withType(Type.UPDATE).build());
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
    existingDelegate.setStatus(Status.WAITING_FOR_APPROVAL);
    wingsPersistence.save(existingDelegate);

    Delegate updatedDelegate = delegateService.updateApprovalStatus(accountId, delegateId, DelegateApproval.ACTIVATE);

    assertThat(existingDelegate).isEqualToIgnoringGivenFields(updatedDelegate, DelegateKeys.status);
    assertThat(Status.ENABLED).isEqualTo(updatedDelegate.getStatus());
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
    existingDelegate.setStatus(Status.WAITING_FOR_APPROVAL);
    wingsPersistence.save(existingDelegate);

    Delegate updatedDelegate = delegateService.updateApprovalStatus(accountId, delegateId, DelegateApproval.REJECT);

    assertThat(existingDelegate).isEqualToIgnoringGivenFields(updatedDelegate, DelegateKeys.status);
    assertThat(Status.DELETED).isEqualTo(updatedDelegate.getStatus());
    verify(auditServiceHelper)
        .reportForAuditingUsingAccountId(accountId, existingDelegate, updatedDelegate, Type.DELEGATE_APPROVAL);
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
    wingsPersistence.save(delegate);
    delegate.setLastHeartBeat(System.currentTimeMillis());
    delegate.setStatus(Status.DISABLED);
    delegateService.update(delegate);
    Delegate updatedDelegate = wingsPersistence.get(Delegate.class, delegate.getUuid());
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

    assertThat(wingsPersistence.get(Delegate.class, delegate.getUuid())).isEqualTo(delegate);
    verify(eventEmitter)
        .send(Channel.DELEGATES,
            anEvent().withOrgId(accountId).withUuid(delegate.getUuid()).withType(Type.CREATE).build());
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
    assertThat(delegate.getStatus()).isEqualTo(Status.WAITING_FOR_APPROVAL);
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
    when(delegateProfileService.fetchPrimaryProfile(delegateWithoutProfile.getAccountId()))
        .thenReturn(primaryDelegateProfile);

    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    delegateWithoutProfile = delegateService.add(delegateWithoutProfile);

    Delegate savedDelegate = wingsPersistence.get(Delegate.class, delegateWithoutProfile.getUuid());
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

    savedDelegate = wingsPersistence.get(Delegate.class, delegateWithNonExistingProfile.getUuid());
    assertThat(savedDelegate)
        .isEqualToIgnoringGivenFields(delegateWithNonExistingProfile, DelegateKeys.delegateProfileId);
    assertThat(savedDelegate.getDelegateProfileId()).isEqualTo(primaryDelegateProfile.getUuid());
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
    when(delegateProfileService.fetchPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);

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
    String id = wingsPersistence.save(createDelegateBuilder().build());
    delegateService.delete(ACCOUNT_ID, id);
    assertThat(wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.accountId, ACCOUNT_ID).asList())
        .hasSize(0);
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
    when(delegateProfileService.fetchPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);
    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    DelegateRegisterResponse registerResponse = delegateService.register(delegate);
    Delegate delegateFromDb = delegateService.get(accountId, registerResponse.getDelegateId(), true);
    assertThat(delegateFromDb).isEqualTo(delegate);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldRegisterDelegateParams() {
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
                                .polllingModeEnabled(true)
                                .sampleDelegate(true)
                                .build();

    DelegateProfile profile = createDelegateProfileBuilder().accountId(accountId).primary(true).build();
    when(delegateProfileService.fetchPrimaryProfile(accountId)).thenReturn(profile);
    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    DelegateRegisterResponse registerResponse = delegateService.register(params);
    Delegate delegateFromDb = delegateService.get(accountId, registerResponse.getDelegateId(), true);

    assertThat(delegateFromDb.getAccountId()).isEqualTo(params.getAccountId());
    assertThat(delegateFromDb.getHostName()).isEqualTo(params.getHostName());
    assertThat(delegateFromDb.getDescription()).isEqualTo(params.getDescription());
    assertThat(delegateFromDb.getDelegateType()).isEqualTo(params.getDelegateType());
    assertThat(delegateFromDb.getIp()).isEqualTo(params.getIp());
    assertThat(delegateFromDb.getDelegateGroupName()).isEqualTo(params.getDelegateGroupName());
    assertThat(delegateFromDb.getVersion()).isEqualTo(params.getVersion());
    assertThat(delegateFromDb.isProxy()).isEqualTo(params.isProxy());
    assertThat(delegateFromDb.isPolllingModeEnabled()).isEqualTo(params.isPolllingModeEnabled());
    assertThat(delegateFromDb.isSampleDelegate()).isEqualTo(params.isSampleDelegate());
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
                                .polllingModeEnabled(true)
                                .sampleDelegate(false)
                                .build();
    DelegateProfile profile = createDelegateProfileBuilder().accountId(accountId).primary(true).build();
    when(delegateProfileService.fetchPrimaryProfile(accountId)).thenReturn(profile);
    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    DelegateRegisterResponse registerResponse = delegateService.register(params);
    Delegate delegateFromDb = delegateService.get(accountId, registerResponse.getDelegateId(), true);

    assertThat(delegateFromDb.getAccountId()).isEqualTo(params.getAccountId());
    assertThat(delegateFromDb.getHostName()).isEqualTo(params.getHostName());
    assertThat(delegateFromDb.getDescription()).isEqualTo(params.getDescription());
    assertThat(delegateFromDb.getDelegateType()).isEqualTo(params.getDelegateType());
    assertThat(delegateFromDb.getIp()).isEqualTo(params.getIp());
    assertThat(delegateFromDb.getDelegateGroupName()).isEqualTo(params.getDelegateGroupName());
    assertThat(delegateFromDb.getVersion()).isEqualTo(params.getVersion());
    assertThat(delegateFromDb.isProxy()).isEqualTo(params.isProxy());
    assertThat(delegateFromDb.isPolllingModeEnabled()).isEqualTo(params.isPolllingModeEnabled());
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
    when(delegateProfileService.fetchPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);

    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    delegate = delegateService.add(delegate);
    delegateService.register(delegate);
    Delegate registeredDelegate = delegateService.get(accountId, delegate.getUuid(), true);
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
    when(delegateProfileService.fetchPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);

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
                                .polllingModeEnabled(true)
                                .sampleDelegate(false)
                                .build();

    delegateService.register(params);

    Delegate delegateFromDb = delegateService.get(accountId, delegate.getUuid(), true);
    assertThat(delegateFromDb.getAccountId()).isEqualTo(params.getAccountId());
    assertThat(delegateFromDb.getHostName()).isEqualTo(params.getHostName());
    assertThat(delegateFromDb.getDescription()).isEqualTo(params.getDescription());
    assertThat(delegateFromDb.getDelegateType()).isEqualTo(params.getDelegateType());
    assertThat(delegateFromDb.getIp()).isEqualTo(params.getIp());
    assertThat(delegateFromDb.getDelegateGroupName()).isEqualTo(params.getDelegateGroupName());
    assertThat(delegateFromDb.getVersion()).isEqualTo(params.getVersion());
    assertThat(delegateFromDb.isProxy()).isEqualTo(params.isProxy());
    assertThat(delegateFromDb.isPolllingModeEnabled()).isEqualTo(params.isPolllingModeEnabled());
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
    when(delegateProfileService.fetchPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);

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
                                .polllingModeEnabled(true)
                                .sampleDelegate(false)
                                .build();

    delegateService.register(params);

    Delegate delegateFromDb = delegateService.get(accountId, delegate.getUuid(), true);
    assertThat(delegateFromDb.getAccountId()).isEqualTo(params.getAccountId());
    assertThat(delegateFromDb.getDescription()).isEqualTo(params.getDescription());
    assertThat(delegateFromDb.getDelegateType()).isEqualTo(params.getDelegateType());
    assertThat(delegateFromDb.getIp()).isEqualTo(params.getIp());
    assertThat(delegateFromDb.getDelegateGroupName()).isEqualTo(params.getDelegateGroupName());
    assertThat(delegateFromDb.getVersion()).isEqualTo(params.getVersion());
    assertThat(delegateFromDb.isProxy()).isEqualTo(params.isProxy());
    assertThat(delegateFromDb.isPolllingModeEnabled()).isEqualTo(params.isPolllingModeEnabled());
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
    when(delegateProfileService.fetchPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);
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
      when(delegateProfileService.fetchPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);
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
    when(delegateProfileService.fetchPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);

    when(delegatesFeature.getMaxUsageAllowedForAccount(accountId)).thenReturn(Integer.MAX_VALUE);

    delegate = delegateService.add(delegate);
    DelegateParams params = DelegateParams.builder()
                                .delegateId(delegate.getUuid())
                                .accountId(accountId)
                                .hostName(HOST_NAME + "UPDATED")
                                .version(VERSION + "UPDATED")
                                .proxy(true)
                                .polllingModeEnabled(true)
                                .sampleDelegate(false)
                                .build();

    delegateService.register(params);

    Delegate delegateFromDb = delegateService.get(accountId, delegate.getUuid(), true);
    assertThat(delegateFromDb.getAccountId()).isEqualTo(params.getAccountId());
    assertThat(delegateFromDb.getHostName()).isEqualTo(params.getHostName());
    assertThat(delegateFromDb.getDescription()).isEqualTo(params.getDescription());
    assertThat(delegateFromDb.getDelegateType()).isEqualTo(params.getDelegateType());
    assertThat(delegateFromDb.getIp()).isEqualTo(params.getIp());
    assertThat(delegateFromDb.getDelegateGroupName()).isEqualTo(params.getDelegateGroupName());
    assertThat(delegateFromDb.getVersion()).isEqualTo(params.getVersion());
    assertThat(delegateFromDb.isProxy()).isEqualTo(params.isProxy());
    assertThat(delegateFromDb.isPolllingModeEnabled()).isEqualTo(params.isPolllingModeEnabled());
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
                            .status(Status.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .build();

    DelegateProfile primaryDelegateProfile =
        createDelegateProfileBuilder().accountId(delegate.getAccountId()).primary(true).build();

    delegate.setDelegateProfileId(primaryDelegateProfile.getUuid());
    when(delegateProfileService.fetchPrimaryProfile(delegate.getAccountId())).thenReturn(primaryDelegateProfile);

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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldSaveDelegateTask() {
    DelegateTask delegateTask =
        DelegateTask.builder()
            .uuid(generateUuid())
            .accountId(ACCOUNT_ID)
            .executionCapabilities(
                asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
                    "http://www.url.com")))
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
    assertThat(
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.uuid, delegateTask.getUuid()).get())
        .isEqualTo(delegateTask);
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
                    "http://www.url.com")))
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
    assertThat(
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.uuid, delegateTask.getUuid()).get())
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
    delegateService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build())
            .build());
    assertThat(
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.uuid, delegateTask.getUuid()).get())
        .isEqualTo(null);
    verify(waitNotifyEngine)
        .doneWith(
            delegateTask.getWaitId(), ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldProcessDelegateTaskResponseWithoutWaitId() {
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    delegateService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build())
            .build());
    assertThat(
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.uuid, delegateTask.getUuid()).get())
        .isEqualTo(null);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldProcessSyncDelegateTaskResponse() {
    DelegateTask delegateTask = saveDelegateTask(false, emptySet(), QUEUED);
    delegateService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build())
            .build());
    delegateTask = wingsPersistence.get(DelegateTask.class, delegateTask.getUuid());
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
    wingsPersistence.save(delegateTask);

    when(assignDelegateService.connectedWhitelistedDelegates(any())).thenReturn(asList("delegate1", "delegate2"));

    delegateService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build())
            .responseCode(ResponseCode.RETRY_ON_OTHER_DELEGATE)
            .build());
    DelegateTask updatedDelegateTask =
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.uuid, delegateTask.getUuid()).get();

    assertThat(updatedDelegateTask).isNotNull();
    assertThat(updatedDelegateTask.getDelegateId()).isNull();
    assertThat(updatedDelegateTask.getAlreadyTriedDelegates()).isNotNull();
    assertThat(updatedDelegateTask.getAlreadyTriedDelegates().size()).isEqualTo(1);
    assertThat(updatedDelegateTask.getAlreadyTriedDelegates()).contains(DELEGATE_ID);
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
    wingsPersistence.save(delegateTask);

    when(assignDelegateService.connectedWhitelistedDelegates(any())).thenReturn(asList(DELEGATE_ID));

    delegateService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build())
            .responseCode(ResponseCode.RETRY_ON_OTHER_DELEGATE)
            .build());
    DelegateTask updatedDelegateTask =
        wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.uuid, delegateTask.getUuid()).get();
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
    when(delegateProfileService.fetchPrimaryProfile(ACCOUNT_ID))
        .thenReturn(createDelegateProfileBuilder().uuid(DELEGATE_PROFILE_ID).build());
    File gzipFile = delegateService.downloadScripts(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, DELEGATE_NAME, null);
    verifyDownloadScriptsResult(gzipFile, "/expectedStartOpenJdk.sh", "/expectedDelegateOpenJdk.sh");

    when(delegateProfileService.get(ACCOUNT_ID, "invalidProfile")).thenReturn(null);
    when(delegateProfileService.fetchPrimaryProfile(ACCOUNT_ID))
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
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream(expectedStartFilepath))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/delegate.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).isEqualTo(0755);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream(expectedDelegateFilepath))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/stop.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).isEqualTo(0755);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedStopOpenJdk.sh"))));

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
    when(delegateProfileService.fetchPrimaryProfile(ACCOUNT_ID))
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
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedStartOpenJdk.sh"))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/delegate.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).isEqualTo(0755);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams.toString(
              new InputStreamReader(getClass().getResourceAsStream("/expectedDelegateOpenJdk.sh"))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/stop.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).isEqualTo(0755);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedStopOpenJdk.sh"))));

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
    when(delegateProfileService.fetchPrimaryProfile(ACCOUNT_ID))
        .thenReturn(createDelegateProfileBuilder().uuid(DELEGATE_PROFILE_ID).build());
    File gzipFile = delegateService.downloadDocker(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, DELEGATE_NAME, null);
    verifyDownloadDockerResult(gzipFile, "/expectedLaunchHarnessDelegate.sh");

    when(delegateProfileService.get(ACCOUNT_ID, "invalidProfile")).thenReturn(null);
    when(delegateProfileService.fetchPrimaryProfile(ACCOUNT_ID))
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
          .isEqualTo(CharStreams.toString(
              new InputStreamReader(getClass().getResourceAsStream(expectedLaunchDelegateFilepath))));

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
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, "harness-delegate", "", false);
    File tarFile = File.createTempFile(DELEGATE_DIR, ".tar");
    uncompressGzipFile(gzipFile, tarFile);
    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
      assertThat(tarArchiveInputStream.getNextEntry().getName()).isEqualTo(KUBERNETES_DELEGATE + "/");

      TarArchiveEntry file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(ArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/harness-delegate.yaml");
      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams.toString(
              new InputStreamReader(getClass().getResourceAsStream("/expectedHarnessDelegate.yaml"))));

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
    File gzipFile = delegateService.downloadKubernetes(
        "https://localhost:9090", "https://localhost:7070", ACCOUNT_ID, "harness-delegate", "", true);
    File tarFile = File.createTempFile(DELEGATE_DIR, ".tar");
    uncompressGzipFile(gzipFile, tarFile);
    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
      assertThat(tarArchiveInputStream.getNextEntry().getName()).isEqualTo(KUBERNETES_DELEGATE + "/");

      TarArchiveEntry file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(ArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/harness-delegate.yaml");
      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams.toString(
              new InputStreamReader(getClass().getResourceAsStream("/expectedHarnessDelegateWithCiEnabled.yaml"))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/README.txt");
    }
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldSignalForDelegateUpgradeWhenUpdateIsPresent() throws IOException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    Delegate delegate = createDelegateBuilder().build();
    delegate.setUuid(DELEGATE_ID);
    wingsPersistence.save(delegate);
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
    wingsPersistence.save(delegate);
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
    wingsPersistence.save(delegate);
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
    wingsPersistence.save(delegate);
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
    wingsPersistence.save(delegate);
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
    wingsPersistence.save(delegate);
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
    wingsPersistence.save(delegate);
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    assertThat(delegateService.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID + "1", delegateTask.getUuid())).isNull();
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
    delegate.setStatus(Status.WAITING_FOR_APPROVAL);
    wingsPersistence.save(delegate);

    DelegateTaskPackage delegateTaskPackage =
        delegateService.acquireDelegateTask(accountId, delegateId, generateUuid());
    assertThat(delegateTaskPackage).isNull();

    delegate.setStatus(Status.DELETED);
    wingsPersistence.save(delegate);

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
    wingsPersistence.save(delegate);
    assertThat(delegateService.filter(ACCOUNT_ID, DELEGATE_ID)).isFalse();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFilterTaskForAccountOnAbort() {
    Delegate delegate = createDelegateBuilder().build();
    delegate.setAccountId(ACCOUNT_ID + "1");
    delegate.setUuid(DELEGATE_ID);
    wingsPersistence.save(delegate);
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
    wingsPersistence.save(delegate);
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

    delegateService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder().accountId(ACCOUNT_ID).response(jenkinsExecutionResponse).build());
    DelegateTaskNotifyResponseData delegateTaskNotifyResponseData = jenkinsExecutionResponse;
    assertThat(delegateTaskNotifyResponseData.getDelegateMetaInfo().getHostName()).isEqualTo(HOST_NAME);
    assertThat(delegateTaskNotifyResponseData.getDelegateMetaInfo().getId()).isEqualTo(DELEGATE_ID);

    jenkinsExecutionResponse = JenkinsExecutionResponse.builder().delegateMetaInfo(delegateMetaInfo).build();
    delegateTaskNotifyResponseData = jenkinsExecutionResponse;
    wingsPersistence.save(delegateTask);
    delegateService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
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
    deletedDelegate.setStatus(Status.DELETED);
    deletedDelegate.setAccountId(accountId);
    deletedDelegate.setUuid(generateUuid());
    wingsPersistence.save(deletedDelegate);

    DelegateProfileParams delegateProfileParams =
        delegateService.checkForProfile(accountId, deletedDelegate.getUuid(), "", 0);
    assertThat(delegateProfileParams).isNull();

    Delegate wapprDelegate = createDelegateBuilder().build();
    wapprDelegate.setStatus(Status.WAITING_FOR_APPROVAL);
    wapprDelegate.setAccountId(accountId);
    wapprDelegate.setUuid(generateUuid());
    wingsPersistence.save(wapprDelegate);

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
    wingsPersistence.save(delegate);
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
    wingsPersistence.save(delegate);
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
    wingsPersistence.save(previousDelegate);

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

    Delegate delegate = wingsPersistence.get(Delegate.class, DELEGATE_ID);
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
    wingsPersistence.save(previousDelegate);

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

    Delegate delegate = wingsPersistence.get(Delegate.class, DELEGATE_ID);
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
    wingsPersistence.save(delegate);

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
                            .status(Status.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .build();
    wingsPersistence.save(delegate);
    delegate = Delegate.builder()
                   .accountId(ACCOUNT_ID)
                   .ip("127.0.0.1")
                   .hostName("d.e.f")
                   .delegateName("k8s-name")
                   .version(VERSION)
                   .status(Status.ENABLED)
                   .lastHeartBeat(System.currentTimeMillis())
                   .build();
    wingsPersistence.save(delegate);
    List<String> k8sNames = delegateService.getKubernetesDelegateNames(ACCOUNT_ID);
    assertThat(k8sNames.size()).isEqualTo(1);
    assertThat(k8sNames.get(0)).isEqualTo("k8s-name");
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetAllDelegateSelectors() {
    DelegateProfile delegateProfile =
        DelegateProfile.builder().uuid(generateUuid()).accountId(ACCOUNT_ID).name("primary").build();

    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .hostName("a.b.c")
                            .delegateName("testDelegateName1")
                            .version(VERSION)
                            .status(Status.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .delegateProfileId(delegateProfile.getUuid())
                            .tags(ImmutableList.of("abc"))
                            .build();
    wingsPersistence.save(delegate);
    delegate = Delegate.builder()
                   .accountId(ACCOUNT_ID)
                   .ip("127.0.0.1")
                   .hostName("d.e.f")
                   .delegateName("testDelegateName2")
                   .version(VERSION)
                   .status(Status.ENABLED)
                   .lastHeartBeat(System.currentTimeMillis())
                   .delegateProfileId(delegateProfile.getUuid())
                   .tags(ImmutableList.of("def"))
                   .build();

    wingsPersistence.save(delegate);

    when(delegateProfileService.get(delegate.getAccountId(), delegateProfile.getUuid())).thenReturn(delegateProfile);
    Set<String> tags = delegateService.getAllDelegateSelectors(ACCOUNT_ID);
    assertThat(tags.size()).isEqualTo(7);
    assertThat(tags).containsExactlyInAnyOrder(
        "abc", "def", "testdelegatename1", "testdelegatename2", "a.b.c", "d.e.f", "primary");
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldGetAllDelegateSelectorsEmptySelectors() {
    DelegateProfile delegateProfile =
        DelegateProfile.builder().uuid(generateUuid()).accountId(ACCOUNT_ID).name("primary").build();

    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .hostName("a.b.c")
                            .delegateName("testDelegateName1")
                            .version(VERSION)
                            .status(Status.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .delegateProfileId(delegateProfile.getUuid())
                            .build();
    wingsPersistence.save(delegate);

    when(delegateProfileService.get(delegate.getAccountId(), delegateProfile.getUuid())).thenReturn(delegateProfile);
    Set<String> tags = delegateService.getAllDelegateSelectors(ACCOUNT_ID);
    assertThat(tags.size()).isEqualTo(3);
    assertThat(tags).containsExactlyInAnyOrder("testdelegatename1", "a.b.c", "primary");
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldRetrieveDelegateSelectors() {
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .version(VERSION)
                            .status(Status.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .tags(ImmutableList.of("abc", "qwe", "xde"))
                            .build();
    wingsPersistence.save(delegate);

    Set<String> tags = delegateService.retrieveDelegateSelectors(delegate);
    assertThat(tags.size()).isEqualTo(3);
    assertThat(tags).containsExactlyInAnyOrder("abc", "qwe", "xde");
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldRetrieveDelegatesImplicitSelectors() {
    DelegateProfile delegateProfile = DelegateProfile.builder()
                                          .uuid(generateUuid())
                                          .accountId(ACCOUNT_ID)
                                          .name("primary")
                                          .selectors(ImmutableList.of("jkl", "fgh"))
                                          .build();

    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .hostName("host")
                            .delegateName("test")
                            .version(VERSION)
                            .status(Status.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .delegateProfileId(delegateProfile.getUuid())
                            .tags(ImmutableList.of("abc", "bbb", "abc"))
                            .build();
    wingsPersistence.save(delegate);

    when(delegateProfileService.get(delegate.getAccountId(), delegateProfile.getUuid())).thenReturn(delegateProfile);

    Set<String> tags = delegateService.retrieveDelegateSelectors(delegate);
    assertThat(tags.size()).isEqualTo(7);
    assertThat(tags).containsExactlyInAnyOrder("abc", "bbb", "host", "primary", "test", "jkl", "fgh");
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldRetrieveDelegateImplicitSelectorsWithDelegateProfileSelectorsOnly() {
    DelegateProfile delegateProfile = DelegateProfile.builder()
                                          .uuid(generateUuid())
                                          .accountId(ACCOUNT_ID)
                                          .selectors(ImmutableList.of("jkl", "fgh"))
                                          .build();
    wingsPersistence.save(delegateProfile);

    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .delegateProfileId(delegateProfile.getUuid())
                            .ip("127.0.0.1")
                            .version(VERSION)
                            .status(Status.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .build();
    wingsPersistence.save(delegate);

    when(delegateProfileService.get(delegate.getAccountId(), delegateProfile.getUuid())).thenReturn(delegateProfile);
    Set<String> selectors = delegateService.retrieveDelegateSelectors(delegate);
    assertThat(selectors.size()).isEqualTo(2);
    assertThat(selectors).containsExactly("fgh", "jkl");
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldRetrieveDelegateSelectors_Empty() {
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .version(VERSION)
                            .status(Status.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .build();
    wingsPersistence.save(delegate);

    Set<String> tags = delegateService.retrieveDelegateSelectors(delegate);
    assertThat(tags.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldRetrieveDelegateImplicitSelectorsWithHostName() {
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .hostName("a.b.c")
                            .version(VERSION)
                            .status(Status.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .build();
    wingsPersistence.save(delegate);

    Set<String> tags = delegateService.retrieveDelegateSelectors(delegate);
    assertThat(tags.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldRetrieveDelegateImplicitSelectorsWithProfileNameAndTags() {
    DelegateProfile delegateProfile =
        DelegateProfile.builder().uuid(generateUuid()).accountId(ACCOUNT_ID).name("profile-name").build();

    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .delegateProfileId(delegateProfile.getUuid())
                            .version(VERSION)
                            .status(Status.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .tags(ImmutableList.of("abc", "qwe"))
                            .build();
    wingsPersistence.save(delegate);

    when(delegateProfileService.get(delegate.getAccountId(), delegateProfile.getUuid())).thenReturn(delegateProfile);
    Set<String> tags = delegateService.retrieveDelegateSelectors(delegate);
    assertThat(tags.size()).isEqualTo(3);
    assertThat(tags).containsExactlyInAnyOrder("abc", "qwe", "profile-name");
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
    assertThat(wingsPersistence.get(DelegateTask.class, delegateTask.getUuid()).getDelegateId()).isEqualTo(DELEGATE_ID);
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
    assertThat(wingsPersistence.createQuery(DelegateTask.class).get()).isNull();
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
        wingsPersistence.createQuery(DelegateSyncTaskResponse.class).get().getResponseData());

    assertThat(notifyResponse.getException().getMessage()).isEqualTo(expectedMessage);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFailIfAllDelegatesFailed_notAll() {
    DelegateTask delegateTask = saveDelegateTask(true, ImmutableSet.of(DELEGATE_ID, "delegate2"), QUEUED);
    delegateService.failIfAllDelegatesFailed(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid());
    assertThat(wingsPersistence.createQuery(DelegateTask.class).get()).isEqualTo(delegateTask);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFailIfAllDelegatesFailed_whitelist() {
    DelegateTask delegateTask = saveDelegateTask(true, ImmutableSet.of(DELEGATE_ID), QUEUED);
    when(assignDelegateService.connectedWhitelistedDelegates(delegateTask)).thenReturn(singletonList("delegate2"));
    delegateService.failIfAllDelegatesFailed(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid());
    verify(assignDelegateService).connectedWhitelistedDelegates(delegateTask);
    assertThat(wingsPersistence.createQuery(DelegateTask.class).get()).isEqualTo(delegateTask);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldExpireTask() {
    DelegateTask delegateTask = saveDelegateTask(true, ImmutableSet.of(DELEGATE_ID), QUEUED);
    delegateService.expireTask(ACCOUNT_ID, delegateTask.getUuid());
    assertThat(wingsPersistence.createQuery(DelegateTask.class).get().getStatus()).isEqualTo(ERROR);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldAbortTask() {
    DelegateTask delegateTask = saveDelegateTask(true, ImmutableSet.of(DELEGATE_ID), QUEUED);

    DelegateTask oldTask = delegateService.abortTask(ACCOUNT_ID, delegateTask.getUuid());

    assertThat(oldTask.getUuid()).isEqualTo(delegateTask.getUuid());
    assertThat(oldTask.getStatus()).isEqualTo(QUEUED);
    assertThat(wingsPersistence.createQuery(DelegateTask.class).get().getStatus()).isEqualTo(ABORTED);
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
    assertThat(wingsPersistence.get(Delegate.class, delegate.getUuid())).isEqualTo(delegate);

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
                            .status(Status.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .tags(ImmutableList.of("abc", "qwe"))
                            .build();

    wingsPersistence.save(delegate);
    when(licenseService.isAccountDeleted(anyString())).thenReturn(true);

    Delegate result = delegateService.updateHeartbeatForDelegateWithPollingEnabled(delegate);

    assertThat(result).extracting(Delegate::getStatus).isEqualTo(Status.DELETED);
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
    wingsPersistence.save(sampleMap);

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

    delegateService.saveDelegateTask(delegateTask, QUEUED);

    if (status != delegateTask.getStatus()) {
      delegateTask = wingsPersistence.findAndModify(
          wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.uuid, delegateTask.getUuid()),
          wingsPersistence.createUpdateOperations(DelegateTask.class).set(DelegateTaskKeys.status, status),
          HPersistence.returnNewOptions);
    }

    return delegateTask;
  }

  private DelegateBuilder createDelegateBuilder() {
    return Delegate.builder()
        .accountId(ACCOUNT_ID)
        .ip("127.0.0.1")
        .hostName("localhost")
        .delegateName("testDelegateName")
        .delegateType("dockerType")
        .version(VERSION)
        .status(Status.ENABLED)
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
