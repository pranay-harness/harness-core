package io.harness.batch.processing.tasklet;

import static io.harness.rule.OwnerRule.UTSAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.StorageResource;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.dao.intfc.PublishedMessageDao;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.category.element.UnitTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.watch.PVEvent;
import io.harness.perpetualtask.k8s.watch.PVInfo;
import io.harness.perpetualtask.k8s.watch.Quantity;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import software.wings.security.authentication.BatchQueryConfig;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class K8sPVInfoEventTaskletTest extends CategoryTest {
  private static final String PV_NAME = "pv-name";
  private static final PVInfo.PVType PV_TYPE = PVInfo.PVType.PV_TYPE_GCE_PERSISTENT_DISK;
  private static final Quantity CAPACITY = Quantity.newBuilder().setAmount(1024 * 1024).build();
  private static final String PV_UID = "pv-uid";
  private static final String CLAIM_NAME = "mongo-data";
  private static final String CLAIM_NAMESPACE = "harness-free";
  private static final String PV_STATUS = "released";
  private static final String ACCOUNT_ID = "account_id";
  private static final Map<String, String> LABELS = ImmutableMap.of("k1", "v1");
  private final Instant NOW = Instant.now();
  private final Timestamp START_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(1, ChronoUnit.DAYS));
  private final Timestamp END_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(10, ChronoUnit.MINUTES));
  private final long START_TIME_MILLIS = NOW.minus(1, ChronoUnit.HOURS).toEpochMilli();
  private final long END_TIME_MILLIS = NOW.toEpochMilli();

  @Mock private BatchMainConfig config;
  @Mock private HPersistence hPersistence;
  @Mock private InstanceDataDao instanceDataDao;
  @Mock private InstanceDataService instanceDataService;
  @Mock private PublishedMessageDao publishedMessageDao;
  @InjectMocks private K8sPVEventTasklet k8sPVEventTasklet;
  @InjectMocks private K8sPVInfoTasklet k8sPVInfoTasklet;

  ChunkContext chunkContext = mock(ChunkContext.class);
  StepContext stepContext = mock(StepContext.class);
  StepExecution stepExecution = mock(StepExecution.class);
  JobParameters parameters = mock(JobParameters.class);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(config.getBatchQueryConfig()).thenReturn(BatchQueryConfig.builder().queryBatchSize(50).build());
    when(chunkContext.getStepContext()).thenReturn(stepContext);
    when(stepContext.getStepExecution()).thenReturn(stepExecution);
    when(stepExecution.getJobParameters()).thenReturn(parameters);
    when(parameters.getString(CCMJobConstants.JOB_START_DATE)).thenReturn(String.valueOf(START_TIME_MILLIS));
    when(parameters.getString(CCMJobConstants.ACCOUNT_ID)).thenReturn(ACCOUNT_ID);
    when(parameters.getString(CCMJobConstants.JOB_END_DATE)).thenReturn(String.valueOf(END_TIME_MILLIS));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testPVInfoExecute() {
    PublishedMessage k8sPVInfoMessage = getK8sPVInfoMessage();
    when(publishedMessageDao.fetchPublishedMessage(any(), eq(EventTypeConstants.K8S_PV_INFO), any(), any(), anyInt()))
        .thenReturn(Arrays.asList(k8sPVInfoMessage));

    RepeatStatus repeatStatus = k8sPVInfoTasklet.execute(null, chunkContext);

    verify(publishedMessageDao, times(1))
        .fetchPublishedMessage(any(), eq(EventTypeConstants.K8S_PV_INFO), any(), any(), anyInt());
    assertThat(repeatStatus).isNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testPVEventExecute() {
    PublishedMessage k8sNodeEventMessage = getK8sPVEventMessage(PVEvent.EventType.EVENT_TYPE_START, START_TIMESTAMP);
    when(publishedMessageDao.fetchPublishedMessage(any(), any(), any(), any(), anyInt()))
        .thenReturn(Arrays.asList(k8sNodeEventMessage));

    RepeatStatus repeatStatus = k8sPVEventTasklet.execute(null, chunkContext);

    verify(publishedMessageDao, times(1))
        .fetchPublishedMessage(any(), eq(EventTypeConstants.K8S_PV_EVENT), any(), any(), anyInt());
    assertThat(repeatStatus).isNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldCreateInstanceStartPVEvent() throws Exception {
    PublishedMessage k8sPVEventMessage = getK8sPVEventMessage(PVEvent.EventType.EVENT_TYPE_START, START_TIMESTAMP);
    InstanceEvent instanceEvent = k8sPVEventTasklet.processPVEventMessage(k8sPVEventMessage);

    assertThat(instanceEvent).isNotNull();
    assertThat(instanceEvent.getInstanceId()).isEqualTo(PV_UID);
    assertThat(instanceEvent.getInstanceName()).isEqualTo(PV_NAME);
    assertThat(instanceEvent.getTimestamp()).isEqualTo(HTimestamps.toInstant(START_TIMESTAMP));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldCreateInstanceEndPVEvent() throws Exception {
    PublishedMessage k8sPVEventMessage = getK8sPVEventMessage(PVEvent.EventType.EVENT_TYPE_STOP, END_TIMESTAMP);
    InstanceEvent instanceEvent = k8sPVEventTasklet.processPVEventMessage(k8sPVEventMessage);

    assertThat(instanceEvent).isNotNull();
    assertThat(instanceEvent.getInstanceId()).isEqualTo(PV_UID);
    assertThat(instanceEvent.getInstanceName()).isEqualTo(PV_NAME);
    assertThat(instanceEvent.getTimestamp()).isEqualTo(HTimestamps.toInstant(END_TIMESTAMP));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldCreateInvalidInstancePVEvent() throws Exception {
    InstanceEvent instanceEvent = k8sPVEventTasklet.processPVEventMessage(null);

    assertThat(instanceEvent).isNotNull();
    assertThat(instanceEvent.getInstanceId()).isNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldCreateInvalidInstancePVInfo() throws Exception {
    when(instanceDataService.fetchInstanceData(eq(ACCOUNT_ID), any(), eq(PV_UID)))
        .thenReturn(InstanceData.builder().build());

    InstanceInfo instanceInfo = k8sPVInfoTasklet.processPVInfoMessage(null);

    assertThat(instanceInfo).isNotNull();
    assertThat(instanceInfo.getInstanceId()).isNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldNotUpdateIfExistsPVInfo() throws Exception {
    when(instanceDataService.fetchInstanceData(eq(ACCOUNT_ID), any(), eq(PV_UID)))
        .thenReturn(InstanceData.builder().build());

    InstanceInfo instanceInfo = k8sPVInfoTasklet.processPVInfoMessage(null);

    assertThat(instanceInfo).isNotNull();
    assertThat(instanceInfo.getInstanceId()).isNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldUpdateIfNotExistsPVInfo() throws Exception {
    when(instanceDataService.fetchInstanceData(eq(ACCOUNT_ID), any(), eq(PV_UID))).thenReturn(null);

    PublishedMessage publishedMessage = getK8sPVInfoMessage();
    InstanceInfo instanceInfo = k8sPVInfoTasklet.processPVInfoMessage(publishedMessage);

    assertThat(instanceInfo).isNotNull();
    assertThat(instanceInfo.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(instanceInfo.getInstanceId()).isEqualTo(PV_UID);
    assertThat(instanceInfo.getInstanceName()).isEqualTo(PV_NAME);
    assertThat(instanceInfo.getStorageResource()).isEqualTo(StorageResource.builder().capacity(1D).build());
  }

  private PublishedMessage getK8sPVInfoMessage() {
    PVInfo pvInfo = PVInfo.newBuilder()
                        .setPvName(PV_NAME)
                        .setPvType(PV_TYPE)
                        .setCapacity(CAPACITY)
                        .setPvUid(PV_UID)
                        .setClaimName(CLAIM_NAME)
                        .setClaimNamespace(CLAIM_NAMESPACE)
                        .setCreationTimestamp(START_TIMESTAMP)
                        .setStatus(PV_STATUS)
                        .putAllLabels(LABELS)
                        .build();
    return getPublishedMessageFromMessage(ACCOUNT_ID, pvInfo);
  }

  private PublishedMessage getK8sPVEventMessage(PVEvent.EventType type, Timestamp timestamp) {
    PVEvent pvEvent =
        PVEvent.newBuilder().setPvUid(PV_UID).setPvName(PV_NAME).setTimestamp(timestamp).setEventType(type).build();
    return getPublishedMessageFromMessage(ACCOUNT_ID, pvEvent);
  }

  private PublishedMessage getPublishedMessageFromMessage(String accountId, Message message) {
    Any payload = Any.pack(message);
    return PublishedMessage.builder()
        .type(message.getClass().getName())
        .data(payload.toByteArray())
        .accountId(accountId)
        .build();
  }
}