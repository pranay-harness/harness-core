package software.wings.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.base.Charsets.UTF_8;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.common.Constants.DELEGATE_DIR;
import static software.wings.common.Constants.DOCKER_DELEGATE;
import static software.wings.common.Constants.KUBERNETES_DELEGATE;
import static software.wings.common.Constants.SELF_DESTRUCT;
import static software.wings.sm.ExecutionStatusData.Builder.anExecutionStatusData;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.inject.Inject;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import freemarker.template.TemplateException;
import io.harness.beans.SearchFilter.Operator;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.DeployMode;
import software.wings.app.FileUploadLimit;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.Base;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateConfiguration;
import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateProfile;
import software.wings.beans.DelegateProfileParams;
import software.wings.beans.DelegateScripts;
import software.wings.beans.DelegateStatus;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskEvent;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.DelegateTaskResponse.ResponseCode;
import software.wings.beans.Event.Type;
import software.wings.beans.FileMetadata;
import software.wings.beans.LicenseInfo;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.beans.ServiceVariable;
import software.wings.beans.TaskType;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DelegateProfileErrorAlert;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Cache;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
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
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.JenkinsState.JenkinsExecutionResponse;
import software.wings.waitnotify.DelegateTaskNotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
public class DelegateServiceTest extends WingsBaseTest {
  private static final Delegate.Builder BUILDER = aDelegate()
                                                      .withAppId(Base.GLOBAL_APP_ID)
                                                      .withAccountId(ACCOUNT_ID)
                                                      .withIp("127.0.0.1")
                                                      .withHostName("localhost")
                                                      .withVersion("1.0.0")
                                                      .withStatus(Status.ENABLED)
                                                      .withLastHeartBeat(System.currentTimeMillis());
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private AccountService accountService;
  @Mock private EventEmitter eventEmitter;
  @Mock private MainConfiguration mainConfiguration;
  @Mock private BroadcasterFactory broadcasterFactory;
  @Mock private Broadcaster broadcaster;
  @Mock private AssignDelegateService assignDelegateService;
  @Mock private DelegateProfileService delegateProfileService;
  @Mock private InfraDownloadService infraDownloadService;
  @Mock private LearningEngineService learningEngineService;
  @Mock private SecretManager secretManager;
  @Mock private ManagerDecryptionService managerDecryptionService;
  @Mock private FileService fileService;
  @Mock private AlertService alertService;

  @Rule public WireMockRule wireMockRule = new WireMockRule(8888);

  @InjectMocks @Inject private DelegateService delegateService;

  @Inject private WingsPersistence wingsPersistence;

  private String verificationServiceSecret;
  private Account account =
      anAccount().withLicenseInfo(LicenseInfo.builder().accountStatus(AccountStatus.ACTIVE).build()).build();

  @Before
  public void setUp() {
    verificationServiceSecret = generateUuid();
    when(mainConfiguration.getDelegateMetadataUrl()).thenReturn("http://localhost:8888/delegateci.txt");
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.KUBERNETES);
    when(mainConfiguration.getWatcherMetadataUrl()).thenReturn("http://localhost:8888/watcherci.txt");
    FileUploadLimit fileUploadLimit = new FileUploadLimit();
    fileUploadLimit.setProfileResultLimit(1000000000L);
    when(mainConfiguration.getFileUploadLimits()).thenReturn(fileUploadLimit);
    when(accountService.getDelegateConfiguration(anyString()))
        .thenReturn(DelegateConfiguration.builder().delegateVersions(singletonList("0.0.0")).build());
    when(accountService.get(ACCOUNT_ID)).thenReturn(account);
    when(infraDownloadService.getDownloadUrlForDelegate(anyString()))
        .thenReturn("http://localhost:8888/builds/9/delegate.jar");
    when(learningEngineService.getServiceSecretKey(ServiceType.LEARNING_ENGINE)).thenReturn(verificationServiceSecret);
    setInternalState(delegateService, "learningEngineService", learningEngineService);
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
  }

  @Test
  public void shouldList() {
    Delegate delegate = wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().build());
    assertThat(delegateService.list(aPageRequest().addFilter(Delegate.ACCOUNT_ID_KEY, Operator.EQ, ACCOUNT_ID).build()))
        .hasSize(1)
        .containsExactly(delegate);
  }

  @Test
  public void shouldGet() {
    Delegate delegate = wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().build());
    assertThat(delegateService.get(ACCOUNT_ID, delegate.getUuid(), true)).isEqualTo(delegate);
  }

  @Test
  public void shouldGetDelegateStatus() {
    when(accountService.getDelegateConfiguration(anyString()))
        .thenReturn(DelegateConfiguration.builder().watcherVersion("1.0.0").delegateVersions(asList("1.0.0")).build());
    Delegate delegate = wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().build());
    DelegateConnection delegateConnection = wingsPersistence.saveAndGet(DelegateConnection.class,
        DelegateConnection.builder().accountId(ACCOUNT_ID).delegateId(delegate.getUuid()).version("1.0.0").build());
    DelegateStatus delegateStatus = delegateService.getDelegateStatus(ACCOUNT_ID);
    assertThat(delegateStatus.getPublishedVersions()).hasSize(1).contains("1.0.0");
    assertThat(delegateStatus.getDelegates()).hasSize(1);
    assertThat(delegateStatus.getDelegates().get(0)).hasFieldOrPropertyWithValue("uuid", delegate.getUuid());
    assertThat(delegateStatus.getDelegates().get(0).getConnections()).hasSize(1);
    assertThat(delegateStatus.getDelegates().get(0).getConnections().get(0))
        .hasFieldOrPropertyWithValue("version", "1.0.0");
  }

  @Test
  public void shouldUpdate() {
    Delegate delegate = wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().build());
    delegate.setLastHeartBeat(System.currentTimeMillis());
    delegate.setStatus(Status.DISABLED);
    delegateService.update(delegate);
    assertThat(wingsPersistence.get(Delegate.class, delegate.getUuid())).isEqualTo(delegate);
    verify(eventEmitter)
        .send(Channel.DELEGATES,
            anEvent().withOrgId(ACCOUNT_ID).withUuid(delegate.getUuid()).withType(Type.UPDATE).build());
  }

  @Test
  public void shouldAdd() {
    Delegate delegate = delegateService.add(BUILDER.but().build());
    assertThat(wingsPersistence.get(Delegate.class, delegate.getUuid())).isEqualTo(delegate);
    verify(eventEmitter)
        .send(Channel.DELEGATES,
            anEvent().withOrgId(ACCOUNT_ID).withUuid(delegate.getUuid()).withType(Type.CREATE).build());
  }

  @Test
  public void shouldDelete() {
    String id = wingsPersistence.save(BUILDER.but().build());
    delegateService.delete(ACCOUNT_ID, id);
    assertThat(wingsPersistence.createQuery(Delegate.class).filter(Delegate.ACCOUNT_ID_KEY, ACCOUNT_ID).asList())
        .hasSize(0);
  }

  @Test
  public void shouldRegister() {
    Delegate delegate = delegateService.register(BUILDER.but().build());
    Delegate delegateFromDb = delegateService.get(ACCOUNT_ID, delegate.getUuid(), true);
    delegateFromDb.setVerificationServiceSecret(verificationServiceSecret);
    assertThat(delegateFromDb).isEqualTo(delegate);
  }

  @Test
  public void shouldRegisterExistingDelegate() {
    Delegate delegate = delegateService.add(BUILDER.but().build());
    delegateService.register(delegate);
    assertThat(delegateService.get(ACCOUNT_ID, delegate.getUuid(), true)).isEqualTo(delegate);
  }

  @Test
  public void shouldNotRegisterExistingDelegateForDeletedAccount() {
    Delegate delegate = delegateService.add(aDelegate()
                                                .withAppId(Base.GLOBAL_APP_ID)
                                                .withAccountId("DELETED_ACCOUNT")
                                                .withIp("127.0.0.1")
                                                .withHostName("localhost")
                                                .withVersion("1.0.0")
                                                .withStatus(Status.ENABLED)
                                                .withLastHeartBeat(System.currentTimeMillis())
                                                .build());
    when(accountService.isAccountDeleted("DELETED_ACCOUNT")).thenReturn(true);

    Delegate registered = delegateService.register(delegate);
    assertThat(registered.getUuid()).isEqualTo(SELF_DESTRUCT);
  }

  @Test
  public void shouldNotRegisterNewDelegateForDeletedAccount() {
    Delegate delegate = aDelegate()
                            .withAppId(Base.GLOBAL_APP_ID)
                            .withAccountId("DELETED_ACCOUNT")
                            .withIp("127.0.0.1")
                            .withHostName("localhost")
                            .withVersion("1.0.0")
                            .withStatus(Status.ENABLED)
                            .withLastHeartBeat(System.currentTimeMillis())
                            .build();
    when(accountService.isAccountDeleted("DELETED_ACCOUNT")).thenReturn(true);

    Delegate registered = delegateService.register(delegate);
    assertThat(registered.getUuid()).isEqualTo(SELF_DESTRUCT);
  }

  @Test
  public void shouldGetDelegateTaskEvents() {
    String delegateId = generateUuid();
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(generateUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withStatus(DelegateTask.Status.QUEUED)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .build();
    wingsPersistence.save(delegateTask);
    List<DelegateTaskEvent> delegateTaskEvents = delegateService.getDelegateTaskEvents(ACCOUNT_ID, delegateId, false);
    assertThat(delegateTaskEvents).hasSize(1);
    assertThat(delegateTaskEvents.get(0).getDelegateTaskId()).isEqualTo(delegateTask.getUuid());
  }

  @Test
  public void shouldSaveDelegateTask() {
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(generateUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .build();
    delegateService.queueTask(delegateTask);
    assertThat(wingsPersistence.get(
                   DelegateTask.class, aPageRequest().addFilter(DelegateTask.APP_ID_KEY, Operator.EQ, APP_ID).build()))
        .isEqualTo(delegateTask);
  }

  @Test
  public void shouldSaveDelegateTaskWithPreAssignedDelegateId() {
    when(assignDelegateService.pickFirstAttemptDelegate(any(DelegateTask.class))).thenReturn(DELEGATE_ID);

    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(generateUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .build();
    delegateService.queueTask(delegateTask);
    DelegateTask delegateTask1 = wingsPersistence.get(
        DelegateTask.class, aPageRequest().addFilter(DelegateTask.APP_ID_KEY, Operator.EQ, APP_ID).build());
    assertThat(delegateTask1.getPreAssignedDelegateId()).isEqualTo(DELEGATE_ID);
  }

  @Test
  public void shouldProcessDelegateTaskResponse() {
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(generateUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .build();
    wingsPersistence.save(delegateTask);
    delegateService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build())
            .build());
    assertThat(wingsPersistence.get(DelegateTask.class,
                   aPageRequest().addFilter(DelegateTask.ID_KEY, Operator.EQ, delegateTask.getUuid()).build()))
        .isEqualTo(null);
    verify(waitNotifyEngine)
        .notify(delegateTask.getWaitId(), anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build());
  }

  @Test
  public void shouldProcessDelegateTaskResponseWithoutWaitId() {
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .build();
    wingsPersistence.save(delegateTask);
    delegateService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build())
            .build());
    assertThat(wingsPersistence.get(DelegateTask.class,
                   aPageRequest().addFilter(DelegateTask.ID_KEY, Operator.EQ, delegateTask.getUuid()).build()))
        .isEqualTo(null);
  }

  @Test
  public void shouldProcessSyncDelegateTaskResponse() {
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .withAsync(false)
                                    .build();
    wingsPersistence.save(delegateTask);
    delegateService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build())
            .build());
    delegateTask = wingsPersistence.get(DelegateTask.class, delegateTask.getUuid());
    assertThat(delegateTask.getStatus()).isEqualTo(DelegateTask.Status.FINISHED);
  }

  @Test
  public void processDelegateTaskResponseShouldRequeueTask() {
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(generateUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withDelegateId(DELEGATE_ID)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .build();
    wingsPersistence.save(delegateTask);

    when(assignDelegateService.connectedWhitelistedDelegates(any())).thenReturn(asList("delegate1", "delegate2"));

    delegateService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build())
            .responseCode(ResponseCode.RETRY_ON_OTHER_DELEGATE)
            .build());
    DelegateTask updatedDelegateTask = wingsPersistence.get(
        DelegateTask.class, aPageRequest().addFilter(DelegateTask.ID_KEY, Operator.EQ, delegateTask.getUuid()).build());

    assertThat(updatedDelegateTask != null);
    assertThat(updatedDelegateTask.getDelegateId() == null);
    assertThat(updatedDelegateTask.getAlreadyTriedDelegates().size() == 1);
    assertThat(updatedDelegateTask.getAlreadyTriedDelegates().contains(DELEGATE_ID));
  }

  @Test
  public void shouldNotRequeueTaskWhenAfterDelegatesAreTried() {
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(generateUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withDelegateId(DELEGATE_ID)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .build();
    wingsPersistence.save(delegateTask);

    when(assignDelegateService.connectedWhitelistedDelegates(any())).thenReturn(asList(DELEGATE_ID));

    delegateService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder()
            .accountId(ACCOUNT_ID)
            .response(anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build())
            .responseCode(ResponseCode.RETRY_ON_OTHER_DELEGATE)
            .build());
    DelegateTask updatedDelegateTask = wingsPersistence.get(
        DelegateTask.class, aPageRequest().addFilter(DelegateTask.ID_KEY, Operator.EQ, delegateTask.getUuid()).build());

    assertThat(updatedDelegateTask).isEqualTo(null);
  }

  @Test
  public void shouldDownloadScripts() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    File gzipFile = delegateService.downloadScripts("https://localhost:9090", "https://localhost:7070", ACCOUNT_ID);
    File tarFile = File.createTempFile(DELEGATE_DIR, ".tar");
    uncompressGzipFile(gzipFile, tarFile);
    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
      assertThat(tarArchiveInputStream.getNextEntry().getName()).isEqualTo(DELEGATE_DIR + "/");

      TarArchiveEntry file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).containsExactly(DELEGATE_DIR + "/start.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).containsExactly(0755);

      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedStart.sh"))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).containsExactly(DELEGATE_DIR + "/delegate.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).containsExactly(0755);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedDelegate.sh"))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).containsExactly(DELEGATE_DIR + "/stop.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).containsExactly(0755);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedStop.sh"))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).containsExactly(DELEGATE_DIR + "/README.txt");

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).containsExactly(DELEGATE_DIR + "/proxy.config");
      buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedProxy.config"))));
    }
  }

  @Test
  public void shouldDownloadDocker() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    File gzipFile = delegateService.downloadDocker("https://localhost:9090", "https://localhost:7070", ACCOUNT_ID);
    File tarFile = File.createTempFile(DELEGATE_DIR, ".tar");
    uncompressGzipFile(gzipFile, tarFile);
    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
      assertThat(tarArchiveInputStream.getNextEntry().getName()).isEqualTo(DOCKER_DELEGATE + "/");

      TarArchiveEntry file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file)
          .extracting(ArchiveEntry::getName)
          .containsExactly(DOCKER_DELEGATE + "/launch-harness-delegate.sh");
      assertThat(file).extracting(TarArchiveEntry::getMode).containsExactly(0755);

      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams.toString(
              new InputStreamReader(getClass().getResourceAsStream("/expectedLaunchHarnessDelegate.sh"))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(ArchiveEntry::getName).containsExactly(DOCKER_DELEGATE + "/README.txt");
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
      assertThat(file)
          .extracting(ArchiveEntry::getName)
          .containsExactly(KUBERNETES_DELEGATE + "/harness-delegate.yaml");
      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(tarArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams.toString(
              new InputStreamReader(getClass().getResourceAsStream("/expectedHarnessDelegate.yaml"))));

      file = (TarArchiveEntry) tarArchiveInputStream.getNextEntry();
      assertThat(file).extracting(TarArchiveEntry::getName).containsExactly(KUBERNETES_DELEGATE + "/README.txt");
    }
  }

  @Test
  public void shouldSignalForDelegateUpgradeWhenUpdateIsPresent() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).build());
    DelegateScripts delegateScripts =
        delegateService.getDelegateScripts(ACCOUNT_ID, "0.0.0", "https://localhost:9090", "https://localhost:7070");
    assertThat(delegateScripts.isDoUpgrade()).isTrue();
  }

  @Test
  public void shouldNotSignalForDelegateUpgradeWhenDelegateIsLatest() throws IOException, TemplateException {
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.AWS);
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).build());
    DelegateScripts delegateScripts =
        delegateService.getDelegateScripts(ACCOUNT_ID, "9.9.9", "https://localhost:9090", "https://localhost:7070");
    assertThat(delegateScripts.isDoUpgrade()).isFalse();
  }

  @Cache
  @Test
  public void shouldAcquireTaskWhenQueued() {
    when(assignDelegateService.isWhitelisted(any(DelegateTask.class), any(String.class))).thenReturn(true);
    when(assignDelegateService.canAssign(any(String.class), any(DelegateTask.class))).thenReturn(true);
    wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).build());
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(generateUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .build();
    wingsPersistence.save(delegateTask);
    assertThat(delegateService.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid())).isNotNull();
  }

  @Cache
  @Test
  public void shouldNotAcquireTaskWhenAlreadyAcquired() {
    wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).build());
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(generateUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .withDelegateId(DELEGATE_ID + "1")
                                    .withStatus(DelegateTask.Status.STARTED)
                                    .build();
    wingsPersistence.save(delegateTask);
    assertThat(delegateService.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid())).isNull();
  }

  @Test
  public void shouldFilterTaskForAccount() {
    wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).build());
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID + "1")
                                    .withWaitId(generateUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .build();
    wingsPersistence.save(delegateTask);
    assertThat(delegateService.filter(DELEGATE_ID, delegateTask)).isFalse();
  }

  @Test
  public void shouldNotFilterTaskWhenItMatchesDelegateCriteria() {
    wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).build());
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(generateUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .build();
    wingsPersistence.save(delegateTask);
    assertThat(delegateService.filter(DELEGATE_ID, delegateTask)).isTrue();
  }

  @Test
  public void shouldGetLatestVersion() {
    assertThat(delegateService.getLatestDelegateVersion(ACCOUNT_ID)).isEqualTo("9.9.9");
  }

  @Test
  public void testProcessDelegateTaskResponseWithDelegateMetaInfo() {
    Delegate delegate = aDelegate().withUuid(DELEGATE_ID).withHostName(USER_NAME).build();

    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(generateUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .build();

    JenkinsExecutionResponse jenkinsExecutionResponse = new JenkinsExecutionResponse();

    wingsPersistence.save(delegate);
    wingsPersistence.save(delegateTask);
    delegateService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder().accountId(ACCOUNT_ID).response(jenkinsExecutionResponse).build());
    DelegateTaskNotifyResponseData delegateTaskNotifyResponseData = jenkinsExecutionResponse;
    assertThat(delegateTaskNotifyResponseData.getDelegateMetaInfo().getHostName()).isEqualTo(USER_NAME);
    assertThat(delegateTaskNotifyResponseData.getDelegateMetaInfo().getId()).isEqualTo(DELEGATE_ID);

    jenkinsExecutionResponse = new JenkinsExecutionResponse();
    delegateTaskNotifyResponseData = jenkinsExecutionResponse;
    wingsPersistence.save(delegateTask);
    wingsPersistence.delete(delegate);
    delegateService.processDelegateResponse(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid(),
        DelegateTaskResponse.builder().accountId(ACCOUNT_ID).response(jenkinsExecutionResponse).build());
    assertThat(delegateTaskNotifyResponseData.getDelegateMetaInfo().getId()).isEqualTo(DELEGATE_ID);
  }

  @Test
  public void shouldCheckForProfile() {
    Delegate delegate =
        aDelegate().withUuid(DELEGATE_ID).withAccountId(ACCOUNT_ID).withDelegateProfileId("profile1").build();
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
  public void shouldCheckForProfileWithSecrets() {
    EncryptedData encryptedData = EncryptedData.builder().build();
    encryptedData.setUuid(generateUuid());
    List<EncryptedDataDetail> encryptionDetails =
        ImmutableList.of(EncryptedDataDetail.builder().encryptedData(encryptedData).build());
    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .accountId(ACCOUNT_ID)
                                          .type(ENCRYPTED_TEXT)
                                          .encryptedValue(encryptedData.getUuid())
                                          .secretTextName("My Secret")
                                          .build();
    when(secretManager.getSecretByName(ACCOUNT_ID, "My Secret", true)).thenReturn(encryptedData);
    when(secretManager.getEncryptionDetails(eq(serviceVariable), eq(null), eq(null))).thenReturn(encryptionDetails);

    Delegate delegate =
        aDelegate().withUuid(DELEGATE_ID).withAccountId(ACCOUNT_ID).withDelegateProfileId("profileSecret").build();
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
  public void shouldSaveProfileResult_NoPrevious() {
    Delegate previousDelegate =
        aDelegate().withUuid(DELEGATE_ID).withAccountId(ACCOUNT_ID).withHostName("hostname").withIp("1.2.3.4").build();
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
            eq(DelegateProfileErrorAlert.builder().accountId(ACCOUNT_ID).hostName("hostname").ip("1.2.3.4").build()));

    Delegate delegate = wingsPersistence.get(Delegate.class, DELEGATE_ID);
    assertThat(delegate.getProfileExecutedAt()).isGreaterThanOrEqualTo(now);
    assertThat(delegate.isProfileError()).isFalse();
    assertThat(delegate.getProfileResult()).isEqualTo("file_id");
  }

  @Test
  public void shouldSaveProfileResult_WithPrevious() {
    Delegate previousDelegate =
        aDelegate().withUuid(DELEGATE_ID).withAccountId(ACCOUNT_ID).withHostName("hostname").withIp("1.2.3.4").build();
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
            eq(DelegateProfileErrorAlert.builder().accountId(ACCOUNT_ID).hostName("hostname").ip("1.2.3.4").build()));

    verify(fileService).deleteFile(eq("previous-result"), eq(FileBucket.PROFILE_RESULTS));

    Delegate delegate = wingsPersistence.get(Delegate.class, DELEGATE_ID);
    assertThat(delegate.getProfileExecutedAt()).isGreaterThanOrEqualTo(now);
    assertThat(delegate.isProfileError()).isTrue();
    assertThat(delegate.getProfileResult()).isEqualTo("file_id");
  }

  @Test
  public void shouldGetProfileResult() {
    Delegate delegate =
        aDelegate().withUuid(DELEGATE_ID).withAccountId(ACCOUNT_ID).withHostName("hostname").withIp("1.2.3.4").build();
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
}
