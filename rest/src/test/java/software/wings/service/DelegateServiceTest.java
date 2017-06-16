package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.DelegateTaskResponse.Builder.aDelegateTaskResponse;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.ExecutionStatusData.Builder.anExecutionStatusData;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import com.google.common.io.CharStreams;

import org.apache.commons.compress.archivers.zip.AsiExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipExtraField;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.Base;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateTask;
import software.wings.beans.Event.Type;
import software.wings.beans.TaskType;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Cache;
import software.wings.rules.RepeatRule.Repeat;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.CacheHelper;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import javax.inject.Inject;

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
  @Mock private CacheHelper cacheHelper;
  @Mock private javax.cache.Cache<String, DelegateTask> cache;

  @InjectMocks @Inject private DelegateService delegateService;
  @Inject private WingsPersistence wingsPersistence;

  @Before
  public void setUp() {
    when(mainConfiguration.getDelegateMetadataUrl())
        .thenReturn("http://wingsdelegates.s3-website-us-east-1.amazonaws.com/delegateci.txt");
    when(broadcasterFactory.lookup(anyString(), anyBoolean())).thenReturn(broadcaster);
    when(cacheHelper.getCache("delegateSyncCache", String.class, DelegateTask.class)).thenReturn(cache);
  }

  @Test
  public void shouldList() throws Exception {
    Delegate delegate = wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().build());
    assertThat(delegateService.list(aPageRequest().build())).hasSize(1).containsExactly(delegate);
  }

  @Test
  public void shouldGet() throws Exception {
    Delegate delegate = wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().build());
    assertThat(delegateService.get(ACCOUNT_ID, delegate.getUuid())).isEqualTo(delegate);
  }

  @Test
  public void shouldUpdate() throws Exception {
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
  public void shouldAdd() throws Exception {
    Delegate delegate = delegateService.add(BUILDER.but().build());
    assertThat(wingsPersistence.get(Delegate.class, delegate.getUuid())).isEqualTo(delegate);
    verify(eventEmitter)
        .send(Channel.DELEGATES,
            anEvent().withOrgId(ACCOUNT_ID).withUuid(delegate.getUuid()).withType(Type.CREATE).build());
  }

  @Test
  public void shouldDelete() throws Exception {
    String id = wingsPersistence.save(BUILDER.but().build());
    delegateService.delete(ACCOUNT_ID, id);
    assertThat(wingsPersistence.list(Delegate.class)).hasSize(0);
  }

  @Test
  public void shouldRegister() throws Exception {
    Delegate delegate = delegateService.register(BUILDER.but().build());
    assertThat(delegateService.get(ACCOUNT_ID, delegate.getUuid())).isEqualTo(delegate);
  }

  @Test
  public void shouldRegisterExistingDelegate() throws Exception {
    Delegate delegate = delegateService.add(BUILDER.but().build());
    delegateService.register(delegate);
    assertThat(delegateService.get(ACCOUNT_ID, delegate.getUuid())).isEqualTo(delegate);
  }

  @Test
  public void shouldSaveDelegateTask() throws Exception {
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(UUIDGenerator.getUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .build();
    delegateService.queueTask(delegateTask);
    assertThat(wingsPersistence.get(DelegateTask.class, aPageRequest().build())).isEqualTo(delegateTask);
  }

  @Test
  public void shouldGetDelegateTasks() throws Exception {
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(UUIDGenerator.getUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .build();
    wingsPersistence.save(delegateTask);
    assertThat(delegateService.getDelegateTasks(ACCOUNT_ID, UUIDGenerator.getUuid()))
        .hasSize(1)
        .containsExactly(delegateTask);
  }

  @Test
  public void shouldProcessDelegateTaskResponse() throws Exception {
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(UUIDGenerator.getUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .build();
    wingsPersistence.save(delegateTask);
    delegateService.processDelegateResponse(
        aDelegateTaskResponse()
            .withAccountId(ACCOUNT_ID)
            .withTask(delegateTask)
            .withResponse(anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build())
            .build());
    assertThat(delegateService.getDelegateTasks(ACCOUNT_ID, UUIDGenerator.getUuid())).isEmpty();
    verify(waitNotifyEngine)
        .notify(delegateTask.getWaitId(), anExecutionStatusData().withExecutionStatus(ExecutionStatus.SUCCESS).build());
  }

  @Test
  @Ignore // TODO:: Delegate refactoring. remove it.
  public void shouldDownloadDelegate() throws Exception {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    File zipFile = delegateService.download("https://localhost:9090", ACCOUNT_ID);
    try (ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(new FileInputStream(zipFile))) {
      assertThat(zipArchiveInputStream.getNextZipEntry().getName()).isEqualTo(Constants.DELEGATE_DIR + "/");

      ZipArchiveEntry file = zipArchiveInputStream.getNextZipEntry();
      assertThat(file).extracting(ZipArchiveEntry::getName).containsExactly(Constants.DELEGATE_DIR + "/run.sh");
      assertThat(file)
          .extracting(ZipArchiveEntry::getExtraFields)
          .flatExtracting(input -> Arrays.asList((ZipExtraField[]) input))
          .extracting(o -> ((AsiExtraField) o).getMode())
          .containsExactly(0755 | AsiExtraField.FILE_FLAG);

      byte[] buffer = new byte[(int) file.getSize()];
      IOUtils.read(zipArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedDelegateRun.sh"))));

      file = zipArchiveInputStream.getNextZipEntry();
      assertThat(file).extracting(ZipArchiveEntry::getName).containsExactly(Constants.DELEGATE_DIR + "/stop.sh");
      assertThat(file)
          .extracting(ZipArchiveEntry::getExtraFields)
          .flatExtracting(input -> Arrays.asList((ZipExtraField[]) input))
          .extracting(o -> ((AsiExtraField) o).getMode())
          .containsExactly(0755 | AsiExtraField.FILE_FLAG);

      buffer = new byte[(int) file.getSize()];
      IOUtils.read(zipArchiveInputStream, buffer);
      assertThat(new String(buffer))
          .isEqualTo(
              CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedDelegateStop.sh"))));
    }
  }

  @Test
  @Repeat(times = 2, successes = 1)
  @Ignore // TODO:: Delegate refactoring. remove it.
  public void shouldSignalForDelegateUpgradeWhenUpdateIsPresent() throws Exception {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).build());
    Delegate delegate = delegateService.checkForUpgrade(ACCOUNT_ID, DELEGATE_ID, "0.0.0", "https://localhost:9090");
    assertThat(delegate.isDoUpgrade()).isTrue();
    assertThat(delegate.getUpgradeScript())
        .isEqualTo(CharStreams.toString(
            new InputStreamReader(getClass().getResourceAsStream("/expectedDelegateUpgradeScript.sh"))));
  }

  @Test
  @Ignore // TODO:: Delegate refactoring. remove it.
  public void shouldNotSignalForDelegateUpgradeWhenDelegateIsLatest() throws Exception {
    when(accountService.get(ACCOUNT_ID))
        .thenReturn(anAccount().withAccountKey("ACCOUNT_KEY").withUuid(ACCOUNT_ID).build());
    wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).build());
    Delegate delegate = delegateService.checkForUpgrade(ACCOUNT_ID, DELEGATE_ID, "0.0.0", "https://localhost:9090");
    assertThat(delegate.isDoUpgrade()).isFalse();
  }

  @Cache
  @Test
  public void shouldAcquireTaskWhenQueued() throws Exception {
    wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).build());
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(UUIDGenerator.getUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .build();
    wingsPersistence.save(delegateTask);
    assertThat(delegateService.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid())).isNotNull();
  }

  @Cache
  @Test
  public void shouldNotAcquireTaskWhenAlreadyAcquired() throws Exception {
    wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).build());
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(UUIDGenerator.getUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withDelegateId(DELEGATE_ID + "1")
                                    .withStatus(DelegateTask.Status.STARTED)
                                    .build();
    wingsPersistence.save(delegateTask);
    assertThat(delegateService.acquireDelegateTask(ACCOUNT_ID, DELEGATE_ID, delegateTask.getUuid())).isNull();
  }

  @Test
  public void shouldFilterTaskForAccount() throws Exception {
    wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).build());
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID + "1")
                                    .withWaitId(UUIDGenerator.getUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .build();
    wingsPersistence.save(delegateTask);
    assertThat(delegateService.filter(DELEGATE_ID, delegateTask)).isFalse();
  }

  @Test
  public void shouldFilterTaskWhenDelegateIsDisabled() throws Exception {
    wingsPersistence.saveAndGet(
        Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).withStatus(Status.DISABLED).build());
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(UUIDGenerator.getUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withDelegateId(DELEGATE_ID)
                                    .withStatus(DelegateTask.Status.STARTED)
                                    .build();
    wingsPersistence.save(delegateTask);
    assertThat(delegateService.filter(DELEGATE_ID, delegateTask)).isFalse();
  }

  @Test
  public void shouldNotFilterTaskWhenItMatchesDelegateCriteria() throws Exception {
    wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).build());
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(UUIDGenerator.getUuid())
                                    .withTaskType(TaskType.HTTP)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .build();
    wingsPersistence.save(delegateTask);
    assertThat(delegateService.filter(DELEGATE_ID, delegateTask)).isTrue();
  }

  @Test
  public void shouldFilterTaskWhenDelegateIsNotCapable() throws Exception {
    wingsPersistence.saveAndGet(
        Delegate.class, BUILDER.but().withUuid(DELEGATE_ID).withStatus(Status.DISABLED).build());
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(ACCOUNT_ID)
                                    .withWaitId(UUIDGenerator.getUuid())
                                    .withTaskType(TaskType.COMMAND)
                                    .withAppId(APP_ID)
                                    .withParameters(new Object[] {})
                                    .withDelegateId(DELEGATE_ID)
                                    .withStatus(DelegateTask.Status.STARTED)
                                    .build();
    wingsPersistence.save(delegateTask);
    assertThat(delegateService.filter(DELEGATE_ID, delegateTask)).isFalse();
  }
}
