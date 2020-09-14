package io.harness.batch.processing.writer;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Any;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.dao.intfc.PublishedMessageDao;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.tasklet.K8SSyncEventTasklet;
import io.harness.category.element.UnitTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.watch.K8SClusterSyncEvent;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import software.wings.security.authentication.BatchQueryConfig;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class K8SSyncEventWriterTest extends CategoryTest {
  private final String TEST_ACCOUNT_ID = "K8S_INSTANCE_INFO_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ID = "K8S_TEST_CLUSTER_ID_" + this.getClass().getSimpleName();
  private final String CLOUD_PROVIDER_ID = "K8S_CLOUD_PROVIDER_ID_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID_NODE_RUNNING =
      "K8S_INSTANCE_INFO_INSTANCE_ID_NODE_RUNNING_" + this.getClass().getSimpleName();
  private final String TEST_ACTIVE_INSTANCE_ID =
      "K8S_INSTANCE_INFO_ACTIVE_INSTANCE_ID_" + this.getClass().getSimpleName();
  private static final String ACCOUNT_ID = "account_id";

  @InjectMocks private K8SSyncEventTasklet k8SSyncEventTasklet;

  @Mock private BatchMainConfig config;

  @Mock private InstanceDataService instanceDataService;

  @Mock private HPersistence hPersistence;

  @Mock private PublishedMessageDao publishedMessageDao;

  private final Instant NOW = Instant.now();
  private final Instant INSTANCE_STOP_TIMESTAMP = NOW.minus(1, ChronoUnit.DAYS);
  private final Instant INSTANCE_START_TIMESTAMP = NOW.minus(2, ChronoUnit.DAYS);
  private final long START_TIME_MILLIS = INSTANCE_START_TIMESTAMP.toEpochMilli();
  private final long END_TIME_MILLIS = INSTANCE_STOP_TIMESTAMP.toEpochMilli();

  @Before
  public void setUpData() {
    MockitoAnnotations.initMocks(this);
    when(config.getBatchQueryConfig()).thenReturn(BatchQueryConfig.builder().queryBatchSize(50).build());
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldStopK8SInstances() {
    ChunkContext chunkContext = mock(ChunkContext.class);
    StepContext stepContext = mock(StepContext.class);
    StepExecution stepExecution = mock(StepExecution.class);
    JobParameters parameters = mock(JobParameters.class);
    when(chunkContext.getStepContext()).thenReturn(stepContext);
    when(stepContext.getStepExecution()).thenReturn(stepExecution);
    when(stepExecution.getJobParameters()).thenReturn(parameters);

    when(parameters.getString(CCMJobConstants.JOB_START_DATE)).thenReturn(String.valueOf(START_TIME_MILLIS));
    when(parameters.getString(CCMJobConstants.ACCOUNT_ID)).thenReturn(ACCOUNT_ID);
    when(parameters.getString(CCMJobConstants.JOB_END_DATE)).thenReturn(String.valueOf(END_TIME_MILLIS));
    when(publishedMessageDao.fetchPublishedMessage(any(), any(), any(), any(), anyInt()))
        .thenReturn(Arrays.asList(k8sSyncEvent()));
    when(instanceDataService.fetchClusterActiveInstanceData(any(), any(), any()))
        .thenReturn(Arrays.asList(getInstanceData(TEST_INSTANCE_ID_NODE_RUNNING)));
    when(instanceDataService.fetchActiveInstanceData(any(), any(), any(), any()))
        .thenReturn(getInstanceData(TEST_INSTANCE_ID_NODE_RUNNING));

    k8SSyncEventTasklet.execute(null, chunkContext);
    ArgumentCaptor<InstanceData> instanceDataArgumentCaptor = ArgumentCaptor.forClass(InstanceData.class);
    ArgumentCaptor<Instant> instantArgumentCaptor = ArgumentCaptor.forClass(Instant.class);
    ArgumentCaptor<InstanceState> instanceStateArgumentCaptor = ArgumentCaptor.forClass(InstanceState.class);
    verify(instanceDataService)
        .updateInstanceState(instanceDataArgumentCaptor.capture(), instantArgumentCaptor.capture(),
            instanceStateArgumentCaptor.capture());
    InstanceData instanceData = instanceDataArgumentCaptor.getValue();
    assertThat(instanceData.getInstanceId()).isEqualTo(TEST_INSTANCE_ID_NODE_RUNNING);
    assertThat(instanceStateArgumentCaptor.getValue()).isEqualTo(InstanceState.STOPPED);
  }

  private InstanceData getInstanceData(String instanceId) {
    return InstanceData.builder().instanceId(instanceId).build();
  }

  private PublishedMessage k8sSyncEvent() {
    K8SClusterSyncEvent k8SClusterSyncEvent = K8SClusterSyncEvent.newBuilder()
                                                  .setClusterId(TEST_CLUSTER_ID)
                                                  .setCloudProviderId(CLOUD_PROVIDER_ID)
                                                  .addAllActiveNodeUids(Arrays.asList(TEST_ACTIVE_INSTANCE_ID))
                                                  .setLastProcessedTimestamp(HTimestamps.fromInstant(NOW))
                                                  .build();
    Any payload = Any.pack(k8SClusterSyncEvent);
    return PublishedMessage.builder()
        .accountId(TEST_ACCOUNT_ID)
        .data(payload.toByteArray())
        .type(k8SClusterSyncEvent.getClass().getName())
        .build();
  }
}
