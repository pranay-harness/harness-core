package software.wings.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.base.Charsets.UTF_8;
import static io.harness.beans.DelegateTask.Status.ABORTED;
import static io.harness.beans.DelegateTask.Status.ERROR;
import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.beans.DelegateTask.Status.STARTED;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.delegate.message.ManagerMessageConstants.SELF_DESTRUCT;
import static io.harness.obfuscate.Obfuscator.obfuscate;
import static io.harness.rule.OwnerRule.ANKIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.PUNEET;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.DelegateConnection.DEFAULT_EXPIRY_TIME_IN_MINUTES;
import static software.wings.beans.DelegateTaskAbortEvent.Builder.aDelegateTaskAbortEvent;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.service.impl.DelegateServiceImpl.DELEGATE_DIR;
import static software.wings.service.impl.DelegateServiceImpl.DOCKER_DELEGATE;
import static software.wings.service.impl.DelegateServiceImpl.ECS;
import static software.wings.service.impl.DelegateServiceImpl.KUBERNETES_DELEGATE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.google.inject.Inject;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import freemarker.template.TemplateException;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.configuration.DeployMode;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.DelegateTaskResponse.ResponseCode;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.version.VersionInfo;
import io.harness.version.VersionInfoManager;
import io.harness.waiter.WaitNotifyEngine;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.FileUploadLimit;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateBuilder;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateConnection.DelegateConnectionKeys;
import software.wings.beans.DelegateConnectionHeartbeat;
import software.wings.beans.DelegateProfile;
import software.wings.beans.DelegateProfileParams;
import software.wings.beans.DelegateStatus;
import software.wings.beans.DelegateTaskEvent;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.Event.Type;
import software.wings.beans.FileMetadata;
import software.wings.beans.LicenseInfo;
import software.wings.beans.ServiceVariable;
import software.wings.beans.TaskType;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DelegateProfileErrorAlert;
import software.wings.delegatetasks.RemoteMethodReturnValueData;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.WingsPersistence;
import software.wings.features.api.UsageLimitedFeature;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.licensing.LicenseService;
import software.wings.rules.Cache;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.impl.infra.InfraDownloadService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionStatusData;
import software.wings.sm.states.JenkinsState.JenkinsExecutionResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

public class DelegateServiceTest extends WingsBaseTest {
  private static final String VERSION = "1.0.0";
  private static final DelegateBuilder BUILDER = Delegate.builder()
                                                     .accountId(ACCOUNT_ID)
                                                     .ip("127.0.0.1")
                                                     .hostName("localhost")
                                                     .version(VERSION)
                                                     .status(Status.ENABLED)
                                                     .lastHeartBeat(System.currentTimeMillis());
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

  @Rule public WireMockRule wireMockRule = new WireMockRule(8888);

  @InjectMocks @Inject private DelegateService delegateService;
  @Mock private UsageLimitedFeature delegatesFeature;

  @Inject private WingsPersistence wingsPersistence;

  private Account account =
      anAccount().withLicenseInfo(LicenseInfo.builder().accountStatus(AccountStatus.ACTIVE).build()).build();

  @Before
  public void setUp() {
    when(subdomainUrlHelper.getDelegateMetadataUrl(any())).thenReturn("http://localhost:8888/delegateci.txt");
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES);
    when(mainConfiguration.getKubectlVersion()).thenReturn("v1.12.2");
    when(mainConfiguration.getOcVersion()).thenReturn("v4.2.16");
    when(subdomainUrlHelper.getWatcherMetadataUrl(any())).thenReturn("http://localhost:8888/watcherci.txt");
    FileUploadLimit fileUploadLimit = new FileUploadLimit();
    fileUploadLimit.setProfileResultLimit(1000000000L);
    when(mainConfiguration.getFileUploadLimits()).thenReturn(fileUploadLimit);
    when(accountService.getDelegateConfiguration(anyString()))
        .thenReturn(DelegateConfiguration.builder().delegateVersions(singletonList("0.0.0")).build());
    when(accountService.get(ACCOUNT_ID)).thenReturn(account);
    when(infraDownloadService.getDownloadUrlForDelegate(anyString(), any()))
        .thenReturn("http://localhost:8888/builds/9/delegate.jar");
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
    when(delegatesFeature.getMaxUsageAllowedForAccount(ACCOUNT_ID)).thenReturn(Integer.MAX_VALUE);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldList() {
    Delegate delegate = BUILDER.build();
    wingsPersistence.save(delegate);
    assertThat(delegateService.list(aPageRequest().addFilter(DelegateKeys.accountId, Operator.EQ, ACCOUNT_ID).build()))
        .hasSize(1)
        .containsExactly(delegate);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGet() {
    Delegate delegate = BUILDER.build();
    wingsPersistence.save(delegate);
    assertThat(delegateService.get(ACCOUNT_ID, delegate.getUuid(), true)).isEqualTo(delegate);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void shouldGetDelegateStatus() {
    when(accountService.getDelegateConfiguration(anyString()))
        .thenReturn(DelegateConfiguration.builder().watcherVersion(VERSION).delegateVersions(asList(VERSION)).build());
    Delegate delegate = BUILDER.build();
    wingsPersistence.save(delegate);
    wingsPersistence.save(
        DelegateConnection.builder().accountId(ACCOUNT_ID).delegateId(delegate.getUuid()).version(VERSION).build());
    DelegateStatus delegateStatus = delegateService.getDelegateStatus(ACCOUNT_ID);
    assertThat(delegateStatus.getPublishedVersions()).hasSize(1).contains(VERSION);
    assertThat(delegateStatus.getDelegates()).hasSize(1);
    assertThat(delegateStatus.getDelegates().get(0)).hasFieldOrPropertyWithValue("uuid", delegate.getUuid());
    assertThat(delegateStatus.getDelegates().get(0).getConnections()).hasSize(1);
    assertThat(delegateStatus.getDelegates().get(0).getConnections().get(0))
        .hasFieldOrPropertyWithValue("version", VERSION);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldUpdate() {
    Delegate delegate = BUILDER.build();
    wingsPersistence.save(delegate);
    delegate.setLastHeartBeat(System.currentTimeMillis());
    delegate.setStatus(Status.DISABLED);
    delegateService.update(delegate);
    Delegate updatedDelegate = wingsPersistence.get(Delegate.class, delegate.getUuid());
    assertThat(updatedDelegate).isEqualTo(delegate);
    verify(eventEmitter)
        .send(Channel.DELEGATES,
            anEvent().withOrgId(ACCOUNT_ID).withUuid(delegate.getUuid()).withType(Type.UPDATE).build());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldUpdateEcs() {
    Delegate delegate = BUILDER.delegateType(ECS).build();
    wingsPersistence.save(delegate);
    delegate.setLastHeartBeat(System.currentTimeMillis());
    delegate.setStatus(Status.DISABLED);
    delegateService.update(delegate);
    Delegate updatedDelegate = wingsPersistence.get(Delegate.class, delegate.getUuid());
    assertThat(updatedDelegate).isEqualTo(delegate);
    verify(eventEmitter)
        .send(Channel.DELEGATES,
            anEvent().withOrgId(ACCOUNT_ID).withUuid(delegate.getUuid()).withType(Type.UPDATE).build());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldAdd() {
    Delegate delegate = BUILDER.build();
    delegate = delegateService.add(delegate);
    assertThat(wingsPersistence.get(Delegate.class, delegate.getUuid())).isEqualTo(delegate);
    verify(eventEmitter)
        .send(Channel.DELEGATES,
            anEvent().withOrgId(ACCOUNT_ID).withUuid(delegate.getUuid()).withType(Type.CREATE).build());
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void shouldNotAddMoreThanAllowedDelegates() {
    int maxDelegatesAllowed = 1;
    when(delegatesFeature.getMaxUsageAllowedForAccount(ACCOUNT_ID)).thenReturn(maxDelegatesAllowed);
    IntStream.range(0, maxDelegatesAllowed).forEach(i -> delegateService.add(BUILDER.build()));
    try {
      delegateService.add(BUILDER.build());
      fail("");
    } catch (WingsException ignored) {
    }
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldDelete() {
    String id = wingsPersistence.save(BUILDER.build());
    delegateService.delete(ACCOUNT_ID, id);
    assertThat(wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.accountId, ACCOUNT_ID).asList())
        .hasSize(0);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldRegister() {
    Delegate delegate = delegateService.register(BUILDER.build());
    Delegate delegateFromDb = delegateService.get(ACCOUNT_ID, delegate.getUuid(), true);
    assertThat(delegateFromDb).isEqualTo(delegate);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldRegisterExistingDelegate() {
    Delegate delegate = delegateService.add(BUILDER.build());
    delegateService.register(delegate);
    Delegate registeredDelegate = delegateService.get(ACCOUNT_ID, delegate.getUuid(), true);
    assertThat(registeredDelegate).isEqualTo(delegate);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotRegisterExistingDelegateForDeletedAccount() {
    Delegate delegate = delegateService.add(Delegate.builder()
                                                .accountId(ACCOUNT_ID)
                                                .ip("127.0.0.1")
                                                .hostName("localhost")
                                                .version(VERSION)
                                                .status(Status.ENABLED)
                                                .lastHeartBeat(System.currentTimeMillis())
                                                .build());
    when(licenseService.isAccountDeleted(ACCOUNT_ID)).thenReturn(true);

    Delegate registered = delegateService.register(delegate);
    assertThat(registered.getUuid()).isEqualTo(SELF_DESTRUCT);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldNotRegisterNewDelegateForDeletedAccount() {
    Delegate delegate = Delegate.builder()
                            .accountId("DELETED_ACCOUNT")
                            .ip("127.0.0.1")
                            .hostName("localhost")
                            .version(VERSION)
                            .status(Status.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .build();
    when(licenseService.isAccountDeleted("DELETED_ACCOUNT")).thenReturn(true);

    Delegate registered = delegateService.register(delegate);
    assertThat(registered.getUuid()).isEqualTo(SELF_DESTRUCT);
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
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .waitId(generateUuid())
                                    .appId(APP_ID)
                                    .version(VERSION)
                                    .data(TaskData.builder()
                                              .taskType(TaskType.HTTP.name())
                                              .parameters(new Object[] {})
                                              .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                              .build())
                                    .build();
    delegateService.queueTask(delegateTask);
    assertThat(wingsPersistence.createQuery(DelegateTask.class).filter(DelegateTaskKeys.appId, APP_ID).get())
        .isEqualTo(delegateTask);
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
    assertThat(delegateTask.getStatus()).isEqualTo(DelegateTask.Status.FINISHED);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void processDelegateTaskResponseShouldRequeueTask() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .waitId(generateUuid())
                                    .delegateId(DELEGATE_ID)
                                    .appId(APP_ID)
                                    .version(VERSION)
                                    .data(TaskData.builder()
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
                                    .async(true)
                                    .accountId(ACCOUNT_ID)
                                    .waitId(generateUuid())
                                    .delegateId(DELEGATE_ID)
                                    .appId(APP_ID)
                                    .version(VERSION)
                                    .data(TaskData.builder()
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
  public void shouldDownloadScripts() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    File gzipFile = delegateService.downloadScripts("https://localhost:9090", "https://localhost:7070", ACCOUNT_ID);
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
          .isEqualTo(CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedStart.sh"))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/delegate.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).isEqualTo(0755);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedDelegate.sh"))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(DELEGATE_DIR + "/stop.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).isEqualTo(0755);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedStop.sh"))));

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
    File gzipFile = delegateService.downloadDocker("https://localhost:9090", "https://localhost:7070", ACCOUNT_ID);
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
              new InputStreamReader(getClass().getResourceAsStream("/expectedLaunchHarnessDelegate.sh"))));

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
          .isEqualTo(CharStreams.toString(
              new InputStreamReader(getClass().getResourceAsStream("/expectedHarnessDelegate.yaml"))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).isEqualTo(KUBERNETES_DELEGATE + "/README.txt");
    }
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldSignalForDelegateUpgradeWhenUpdateIsPresent() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    Delegate delegate = BUILDER.build();
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
    Delegate delegate = BUILDER.build();
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
    when(assignDelegateService.canAssign(any(String.class), any(DelegateTask.class))).thenReturn(true);
    Delegate delegate = BUILDER.build();
    delegate.setUuid(DELEGATE_ID);
    wingsPersistence.save(delegate);
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    assertThat(delegateService.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid())).isNotNull();
  }

  @Cache
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldAcquireTaskWhenQueued_notWhitelisted() {
    when(assignDelegateService.isWhitelisted(any(DelegateTask.class), any(String.class))).thenReturn(false);
    when(assignDelegateService.shouldValidate(any(DelegateTask.class), any(String.class))).thenReturn(true);
    when(assignDelegateService.canAssign(any(String.class), any(DelegateTask.class))).thenReturn(true);
    Delegate delegate = BUILDER.build();
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
    when(assignDelegateService.canAssign(any(String.class), any(DelegateTask.class))).thenReturn(false);
    Delegate delegate = BUILDER.build();
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
    when(assignDelegateService.canAssign(any(String.class), any(DelegateTask.class))).thenReturn(true);
    Delegate delegate = BUILDER.build();
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
    Delegate delegate = BUILDER.build();
    delegate.setUuid(DELEGATE_ID + "1");
    wingsPersistence.save(delegate);
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    assertThat(delegateService.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID + "1", delegateTask.getUuid())).isNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFilterTaskForAccount() {
    Delegate delegate = BUILDER.build();
    delegate.setAccountId(ACCOUNT_ID + "1");
    delegate.setUuid(DELEGATE_ID);
    wingsPersistence.save(delegate);
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    assertThat(delegateService.filter(DELEGATE_ID, delegateTask)).isFalse();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldFilterTaskForAccountOnAbort() {
    Delegate delegate = BUILDER.build();
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
    Delegate delegate = BUILDER.build();
    delegate.setUuid(DELEGATE_ID);
    wingsPersistence.save(delegate);
    DelegateTask delegateTask = saveDelegateTask(true, emptySet(), QUEUED);
    assertThat(delegateService.filter(DELEGATE_ID, delegateTask)).isTrue();
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
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldCheckForProfile() {
    Delegate delegate =
        Delegate.builder().uuid(DELEGATE_ID).accountId(ACCOUNT_ID).delegateProfileId("profile1").build();
    wingsPersistence.save(delegate);
    DelegateProfile profile =
        DelegateProfile.builder().accountId(ACCOUNT_ID).name("A Profile").startupScript("rm -rf /*").build();
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
    DelegateProfile profile = DelegateProfile.builder()
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
  public void shouldGetAllDelegateTags() {
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .hostName("a.b.c")
                            .version(VERSION)
                            .status(Status.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .tags(ImmutableList.of("abc"))
                            .build();
    wingsPersistence.save(delegate);
    delegate = Delegate.builder()
                   .accountId(ACCOUNT_ID)
                   .ip("127.0.0.1")
                   .hostName("d.e.f")
                   .version(VERSION)
                   .status(Status.ENABLED)
                   .lastHeartBeat(System.currentTimeMillis())
                   .tags(ImmutableList.of("def"))
                   .build();
    wingsPersistence.save(delegate);
    Set<String> tags = delegateService.getAllDelegateTags(ACCOUNT_ID);
    assertThat(tags.size()).isEqualTo(2);
    assertThat(tags).containsExactlyInAnyOrder("abc", "def");
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetAllDelegateTags_Empty() {
    Set<String> tags = delegateService.getAllDelegateTags(ACCOUNT_ID);
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
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldDoConnectionHeartbeat() {
    delegateService.doConnectionHeartbeat(
        ACCOUNT_ID, DELEGATE_ID, DelegateConnectionHeartbeat.builder().version("1.0.1").alive(true).build());
    DelegateConnection connection = wingsPersistence.createQuery(DelegateConnection.class)
                                        .filter(DelegateConnectionKeys.accountId, ACCOUNT_ID)
                                        .get();
    assertThat(connection.getVersion()).isEqualTo("1.0.1");
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldRemoveDelegateConnection() {
    DelegateConnection connection =
        DelegateConnection.builder()
            .accountId(ACCOUNT_ID)
            .delegateId(DELEGATE_ID)
            .version("1.0.1")
            .lastHeartbeat(System.currentTimeMillis())
            .validUntil(Date.from(OffsetDateTime.now().plusMinutes(DEFAULT_EXPIRY_TIME_IN_MINUTES).toInstant()))
            .build();
    wingsPersistence.save(connection);

    delegateService.removeDelegateConnection(ACCOUNT_ID, connection.getUuid());
    assertThat(wingsPersistence.createQuery(DelegateConnection.class).get()).isNull();
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
    assertThat(delegateTaskPackage.getDelegateTask().getStatus()).isEqualTo(STARTED);
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
        + "  -  Other delegates (if any) may have been offline or were not eligible due to tag or scope restrictions.";
    RemoteMethodReturnValueData notifyResponse =
        (RemoteMethodReturnValueData) wingsPersistence.createQuery(DelegateTask.class).get().getNotifyResponse();
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
    delegateService.abortTask(ACCOUNT_ID, delegateTask.getUuid());
    assertThat(wingsPersistence.createQuery(DelegateTask.class).get().getStatus()).isEqualTo(ABORTED);
  }

  private DelegateTask saveDelegateTask(boolean async, Set<String> validatingTaskIds, DelegateTask.Status status) {
    DelegateTask delegateTask =
        DelegateTask.builder()
            .async(async)
            .accountId(ACCOUNT_ID)
            .waitId(generateUuid())
            .appId(APP_ID)
            .version(VERSION)
            .status(status)
            .data(TaskData.builder()
                      .taskType(TaskType.HTTP.name())
                      .parameters(new Object[] {HttpTaskParameters.builder().url("https://www.google.com").build()})
                      .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                      .build())
            .validatingDelegateIds(validatingTaskIds)
            .validationCompleteDelegateIds(ImmutableSet.of(DELEGATE_ID))
            .build();
    wingsPersistence.save(delegateTask);
    return delegateTask;
  }
}
