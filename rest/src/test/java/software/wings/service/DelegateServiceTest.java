package software.wings.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.DelegateTaskResponse.Builder.aDelegateTaskResponse;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.sm.ExecutionStatusData.Builder.anExecutionStatusData;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import com.google.common.io.CharStreams;
import com.google.inject.Inject;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import freemarker.template.TemplateException;
import org.apache.commons.compress.archivers.zip.AsiExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipExtraField;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.DeployMode;
import software.wings.app.MainConfiguration;
import software.wings.beans.Base;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateScripts;
import software.wings.beans.DelegateTask;
import software.wings.beans.Event.Type;
import software.wings.beans.TaskType;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Cache;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
                                                      .withSupportedTaskTypes(Lists.newArrayList(TaskType.HTTP))
                                                      .withLastHeartBeat(System.currentTimeMillis());
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private AccountService accountService;
  @Mock private EventEmitter eventEmitter;
  @Mock private MainConfiguration mainConfiguration;
  @Mock private BroadcasterFactory broadcasterFactory;
  @Mock private Broadcaster broadcaster;
  @Mock private AssignDelegateService assignDelegateService;

  @Rule public WireMockRule wireMockRule = new WireMockRule(8888);

  @InjectMocks @Inject private DelegateService delegateService;

  @Inject private WingsPersistence wingsPersistence;

  @Before
  public void setUp() {
    when(mainConfiguration.getDelegateMetadataUrl()).thenReturn("http://localhost:8888/delegateci.txt");
    when(mainConfiguration.getDeployMode()).thenReturn(DeployMode.AWS);
    wireMockRule.stubFor(get(urlEqualTo("/delegateci.txt"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody("9.9.9 jobs/delegateci/9/delegate.jar")
                                             .withHeader("Content-Type", "text/plain")));

    wireMockRule.stubFor(head(urlEqualTo("/jobs/delegateci/9/delegate.jar")).willReturn(aResponse().withStatus(200)));

    when(mainConfiguration.getWatcherMetadataUrl()).thenReturn("http://localhost:8888/watcherci.txt");
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
    assertThat(delegateService.list(aPageRequest().build())).hasSize(1).containsExactly(delegate);
  }

  @Test
  public void shouldGet() {
    Delegate delegate = wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().build());
    assertThat(delegateService.get(ACCOUNT_ID, delegate.getUuid())).isEqualTo(delegate);
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
    assertThat(wingsPersistence.list(Delegate.class)).hasSize(0);
  }

  @Test
  public void shouldRegister() {
    Delegate delegate = delegateService.register(BUILDER.but().build());
    assertThat(delegateService.get(ACCOUNT_ID, delegate.getUuid())).isEqualTo(delegate);
  }

  @Test
  public void shouldRegisterExistingDelegate() {
    Delegate delegate = delegateService.add(BUILDER.but().build());
    delegateService.register(delegate);
    assertThat(delegateService.get(ACCOUNT_ID, delegate.getUuid())).isEqualTo(delegate);
  }

  @Test
  public void shouldGetDelegateTasks() {
    String delegateId = generateUuid();
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(generateUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .withDelegateId(delegateId)
                                    .build();
    wingsPersistence.save(delegateTask);
    assertThat(delegateService.getDelegateTasks(ACCOUNT_ID, delegateId)).hasSize(1).containsExactly(delegateTask);
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
    assertThat(wingsPersistence.get(DelegateTask.class, aPageRequest().build())).isEqualTo(delegateTask);
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
    delegateService.processDelegateResponse(
        aDelegateTaskResponse()
            .withAccountId(ACCOUNT_ID)
            .withTask(delegateTask)
            .withResponse(anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build())
            .build());
    assertThat(delegateService.getDelegateTasks(ACCOUNT_ID, generateUuid())).isEmpty();
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
    delegateService.processDelegateResponse(
        aDelegateTaskResponse()
            .withAccountId(ACCOUNT_ID)
            .withTask(delegateTask)
            .withResponse(anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build())
            .build());
    assertThat(delegateService.getDelegateTasks(ACCOUNT_ID, generateUuid())).isEmpty();
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
    delegateService.processDelegateResponse(
        aDelegateTaskResponse()
            .withAccountId(ACCOUNT_ID)
            .withTask(delegateTask)
            .withResponse(anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build())
            .build());

    delegateTask = wingsPersistence.get(DelegateTask.class, delegateTask.getUuid());
    assertThat(delegateTask.getStatus()).isEqualTo(DelegateTask.Status.FINISHED);
  }

  @Test
  public void shouldDownloadWatcher() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    File zipFile = delegateService.download("https://localhost:9090", ACCOUNT_ID);
    try (ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(new FileInputStream(zipFile))) {
      assertThat(zipArchiveInputStream.getNextZipEntry().getName()).isEqualTo(Constants.DELEGATE_DIR + "/");

      ZipArchiveEntry file = zipArchiveInputStream.getNextZipEntry();
      assertThat(file).extracting(ZipArchiveEntry::getName).containsExactly(Constants.DELEGATE_DIR + "/start.sh");
      assertThat(file)
          .extracting(ZipArchiveEntry::getExtraFields)
          .flatExtracting(input -> asList((ZipExtraField[]) input))
          .extracting(o -> ((AsiExtraField) o).getMode())
          .containsExactly(0755 | AsiExtraField.FILE_FLAG);

      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(zipArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedStart.sh"))));

      file = zipArchiveInputStream.getNextZipEntry();
      assertThat(file).extracting(ZipArchiveEntry::getName).containsExactly(Constants.DELEGATE_DIR + "/delegate.sh");
      assertThat(file)
          .extracting(ZipArchiveEntry::getExtraFields)
          .flatExtracting(input -> asList((ZipExtraField[]) input))
          .extracting(o -> ((AsiExtraField) o).getMode())
          .containsExactly(0755 | AsiExtraField.FILE_FLAG);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(zipArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedDelegate.sh"))));

      file = zipArchiveInputStream.getNextZipEntry();
      assertThat(file).extracting(ZipArchiveEntry::getName).containsExactly(Constants.DELEGATE_DIR + "/stop.sh");
      assertThat(file)
          .extracting(ZipArchiveEntry::getExtraFields)
          .flatExtracting(input -> asList((ZipExtraField[]) input))
          .extracting(o -> ((AsiExtraField) o).getMode())
          .containsExactly(0755 | AsiExtraField.FILE_FLAG);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(zipArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedStop.sh"))));

      file = zipArchiveInputStream.getNextZipEntry();
      assertThat(file).extracting(ZipArchiveEntry::getName).containsExactly(Constants.DELEGATE_DIR + "/README.txt");
      buffer = new byte[(int) file.getSize()];
      IOUtils.read(zipArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedReadme.txt"))));

      file = zipArchiveInputStream.getNextZipEntry();
      assertThat(file).extracting(ZipArchiveEntry::getName).containsExactly(Constants.DELEGATE_DIR + "/proxy.config");
      assertThat(file)
          .extracting(ZipArchiveEntry::getExtraFields)
          .flatExtracting(input -> asList((ZipExtraField[]) input))
          .extracting(o -> ((AsiExtraField) o).getMode())
          .containsExactly(0644 | AsiExtraField.FILE_FLAG);
      buffer = new byte[(int) file.getSize()];
      IOUtils.read(zipArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedProxy.config"))));
    }
  }

  @Test
  public void shouldSignalForDelegateUpgradeWhenUpdateIsPresent() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).build());
    DelegateScripts delegateScripts =
        delegateService.checkForUpgrade(ACCOUNT_ID, DELEGATE_ID, "0.0.0", "https://localhost:9090");
    assertThat(delegateScripts.isDoUpgrade()).isTrue();
  }

  @Test
  public void shouldNotSignalForDelegateUpgradeWhenDelegateIsLatest() throws IOException, TemplateException {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).build());
    DelegateScripts delegateScripts =
        delegateService.checkForUpgrade(ACCOUNT_ID, DELEGATE_ID, "9.9.9", "https://localhost:9090");
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
  public void shouldFilterTaskWhenDelegateIsDisabled() {
    wingsPersistence.saveAndGet(
        Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).withStatus(Status.DISABLED).build());
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(generateUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .withDelegateId(DELEGATE_ID)
                                    .withStatus(DelegateTask.Status.STARTED)
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
  public void shouldFilterTaskWhenDelegateIsNotCapable() {
    wingsPersistence.saveAndGet(
        Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).withStatus(Status.DISABLED).build());
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(generateUuid())
                                    .withTaskType(TaskType.COMMAND)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withTags(new ArrayList<>())
                                    .withDelegateId(DELEGATE_ID)
                                    .withStatus(DelegateTask.Status.STARTED)
                                    .build();
    wingsPersistence.save(delegateTask);
    assertThat(delegateService.filter(DELEGATE_ID, delegateTask)).isFalse();
  }

  @Test
  public void shouldGetLatestVersion() {
    assertThat(delegateService.getLatestDelegateVersion()).isEqualTo("9.9.9");
  }

  @Test
  public void shouldDeleteOldDelegateTasks() {
    wingsPersistence.save(aDelegateTask().withUuid("ID1").withStatus(DelegateTask.Status.ABORTED).build());
    wingsPersistence.save(aDelegateTask().withUuid("ID2").withStatus(DelegateTask.Status.ERROR).build());
    wingsPersistence.save(aDelegateTask().withUuid("ID3").withStatus(DelegateTask.Status.QUEUED).build());
    wingsPersistence.save(aDelegateTask().withUuid("ID4").withStatus(DelegateTask.Status.STARTED).build());
    assertThat(wingsPersistence.createQuery(DelegateTask.class).asList().size()).isEqualTo(4);

    delegateService.deleteOldTasks(0);

    List<DelegateTask> delegateTasks = wingsPersistence.createQuery(DelegateTask.class).asList();
    assertThat(delegateTasks.size()).isEqualTo(0);
  }
}
