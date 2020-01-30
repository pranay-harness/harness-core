package io.harness.perpetualtask.ecs;

import static io.harness.perpetualtask.ecs.EcsPerpetualTaskExecutor.IDENTIFIER_CLUSTER_ID_ATTRIBUTE_NAME;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.HITESH;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.Service;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.client.EventPublisher;
import io.harness.event.payloads.EcsUtilization;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskParams;
import io.harness.perpetualtask.ecs.support.EcsMetricClient;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.beans.AwsConfig;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class EcsPerpetualTaskExecutorTest extends CategoryTest {
  @Mock private AwsEcsHelperServiceDelegate ecsHelperServiceDelegate;
  @Mock private AwsEc2HelperServiceDelegate ec2ServiceDelegate;
  @Mock private EcsMetricClient ecsMetricClient;
  @Mock private EventPublisher eventPublisher;
  private EcsPerpetualTaskExecutor ecsPerpetualTaskExecutor;

  @Captor private ArgumentCaptor<Message> messageCaptor;
  @Captor ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

  private final String REGION = "us-east-1";
  private final String ACCOUNT_ID = "accountId";
  private final String CLUSTER_ID = "clusterId";
  private final String SETTING_ID = "settingId";
  private final String CLUSTER_NAME = "ecs-ccm-cluster";
  private final String PERPETUAL_TASK_ID = "perpetualTaskId";
  private final String CLUSTER_ARN = "arn:aws:ecs:us-east-2:132359207506:cluster/ecs-ccm-cluster";

  @Before
  public void setUp() throws Exception {
    ecsPerpetualTaskExecutor =
        new EcsPerpetualTaskExecutor(ecsHelperServiceDelegate, ec2ServiceDelegate, ecsMetricClient, eventPublisher);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldQueryEcsMetricClientAndPublishUtilizationMessages() throws Exception {
    AwsConfig awsConfig = AwsConfig.builder().build();
    List<EncryptedDataDetail> encryptionDetails = Collections.emptyList();
    final Instant now = Instant.now();
    Instant lastMetricCollectionTime = now.minus(Duration.ofHours(1));
    Instant truncatedPollTime = lastMetricCollectionTime.truncatedTo(HOURS);
    final ImmutableList<Service> services =
        ImmutableList.of(new Service()
                             .withServiceArn("arn:aws:ecs:us-east-2:132359207506:service/ccm-test-service")
                             .withServiceName("ccm-test-service"),
            new Service()
                .withServiceArn("arn:aws:ecs:us-east-1:132359207506:service/ccm-test-service")
                .withServiceName("ccm-test-service"));
    List<EcsUtilization> utilizationMessages =
        ImmutableList.of(EcsUtilization.newBuilder().build(), EcsUtilization.newBuilder().build());

    given(ecsHelperServiceDelegate.listServicesForCluster(awsConfig, encryptionDetails, REGION, CLUSTER_ARN))
        .willReturn(services);
    final EcsPerpetualTaskParams ecsPerpetualTaskParams =
        EcsPerpetualTaskParams.newBuilder().setClusterId(CLUSTER_ID).setSettingId(SETTING_ID).setRegion(REGION).build();
    given(ecsMetricClient.getUtilizationMetrics(eq(awsConfig), eq(encryptionDetails), eq(Date.from(truncatedPollTime)),
              eq(Date.from(now.truncatedTo(HOURS))),
              eq(new Cluster().withClusterName(CLUSTER_NAME).withClusterArn(CLUSTER_ARN)), eq(services),
              eq(ecsPerpetualTaskParams)))
        .willReturn(utilizationMessages);

    ecsPerpetualTaskExecutor.publishUtilizationMetrics(
        ecsPerpetualTaskParams, awsConfig, encryptionDetails, CLUSTER_ARN, lastMetricCollectionTime);

    then(eventPublisher)
        .should(times(2))
        .publishMessage(messageCaptor.capture(), eq(HTimestamps.fromInstant(lastMetricCollectionTime)),
            mapArgumentCaptor.capture());
    assertThat(messageCaptor.getAllValues()).hasSize(2).containsAll(utilizationMessages);
    assertThat(mapArgumentCaptor.getValue().keySet()).contains(IDENTIFIER_CLUSTER_ID_ATTRIBUTE_NAME);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldQueryEcsClientAndPublishSyncMessages() {
    Set<String> activeEc2InstanceIds = new HashSet<>(Arrays.asList("instance1", "instance2"));
    Set<String> activeContainerInstanceArns = new HashSet<>(Collections.singletonList("containerInstance1"));
    Set<String> activeTaskArns = new HashSet<>(Arrays.asList("task1", "task2"));
    Instant pollTime = Instant.now();

    ecsPerpetualTaskExecutor.publishEcsClusterSyncEvent(CLUSTER_ID, SETTING_ID, CLUSTER_NAME, activeEc2InstanceIds,
        activeContainerInstanceArns, activeTaskArns, pollTime);
    then(eventPublisher)
        .should(times(1))
        .publishMessage(messageCaptor.capture(), eq(HTimestamps.fromInstant(pollTime)), mapArgumentCaptor.capture());
    assertThat(messageCaptor.getAllValues()).hasSize(1);
    assertThat(mapArgumentCaptor.getValue().keySet()).contains(IDENTIFIER_CLUSTER_ID_ATTRIBUTE_NAME);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldRunEcsPerpetualTask() {
    Instant heartBeatTime = Instant.now();

    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).build();
    ByteString bytes = ByteString.copyFrom(KryoUtils.asBytes(awsConfig));

    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    List<EncryptedDataDetail> encryptionDetails = new ArrayList<>(Collections.singletonList(encryptedDataDetail));
    ByteString encryptionDetailBytes = ByteString.copyFrom(KryoUtils.asBytes(encryptionDetails));

    EcsPerpetualTaskParams ecsPerpetualTaskParams = EcsPerpetualTaskParams.newBuilder()
                                                        .setClusterId(CLUSTER_ID)
                                                        .setRegion(REGION)
                                                        .setSettingId(SETTING_ID)
                                                        .setClusterName(CLUSTER_NAME)
                                                        .setAwsConfig(bytes)
                                                        .setEncryptionDetail(encryptionDetailBytes)
                                                        .build();

    PerpetualTaskParams params =
        PerpetualTaskParams.newBuilder().setCustomizedParams(Any.pack(ecsPerpetualTaskParams)).build();
    PerpetualTaskId perpetualTaskId = PerpetualTaskId.newBuilder().setId(PERPETUAL_TASK_ID).build();
    boolean runOnce = ecsPerpetualTaskExecutor.runOnce(perpetualTaskId, params, heartBeatTime);
    assertThat(runOnce).isTrue();
  }

  @Test
  @Owner(developers = AVMOHAN, intermittent = true)
  @Category(UnitTests.class)
  public void shouldQuerySingleHourWindowInNormalCase() throws Exception {
    final Instant now = Instant.now();
    Instant heartBeatTime = now.minus(Duration.ofHours(3));
    // lastMetricCollection not within last hour so collect.
    Reflect.on(ecsPerpetualTaskExecutor).set("lastMetricCollectionTime", now.minus(70, MINUTES));
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).build();
    ByteString bytes = ByteString.copyFrom(KryoUtils.asBytes(awsConfig));
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    List<EncryptedDataDetail> encryptionDetails = new ArrayList<>(Collections.singletonList(encryptedDataDetail));
    ByteString encryptionDetailBytes = ByteString.copyFrom(KryoUtils.asBytes(encryptionDetails));
    EcsPerpetualTaskParams ecsPerpetualTaskParams = EcsPerpetualTaskParams.newBuilder()
                                                        .setClusterId(CLUSTER_ID)
                                                        .setRegion(REGION)
                                                        .setSettingId(SETTING_ID)
                                                        .setClusterName(CLUSTER_NAME)
                                                        .setAwsConfig(bytes)
                                                        .setEncryptionDetail(encryptionDetailBytes)
                                                        .build();

    PerpetualTaskParams params =
        PerpetualTaskParams.newBuilder().setCustomizedParams(Any.pack(ecsPerpetualTaskParams)).build();
    PerpetualTaskId perpetualTaskId = PerpetualTaskId.newBuilder().setId(PERPETUAL_TASK_ID).build();
    ecsPerpetualTaskExecutor.runOnce(perpetualTaskId, params, heartBeatTime);

    then(ecsMetricClient)
        .should(times(1))
        .getUtilizationMetrics(any(AwsConfig.class), anyListOf(EncryptedDataDetail.class),
            eq(Date.from(now.minus(Duration.ofHours(1)).truncatedTo(HOURS))), eq(Date.from(now.truncatedTo(HOURS))),
            any(Cluster.class), anyListOf(Service.class), any(EcsPerpetualTaskParams.class));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldQuery24HoursIfNoLastHeartBeat() throws Exception {
    Instant heartBeatTime = Instant.ofEpochMilli(0);
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).build();
    ByteString bytes = ByteString.copyFrom(KryoUtils.asBytes(awsConfig));
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    List<EncryptedDataDetail> encryptionDetails = new ArrayList<>(Collections.singletonList(encryptedDataDetail));
    ByteString encryptionDetailBytes = ByteString.copyFrom(KryoUtils.asBytes(encryptionDetails));
    EcsPerpetualTaskParams ecsPerpetualTaskParams = EcsPerpetualTaskParams.newBuilder()
                                                        .setClusterId(CLUSTER_ID)
                                                        .setRegion(REGION)
                                                        .setSettingId(SETTING_ID)
                                                        .setClusterName(CLUSTER_NAME)
                                                        .setAwsConfig(bytes)
                                                        .setEncryptionDetail(encryptionDetailBytes)
                                                        .build();

    PerpetualTaskParams params =
        PerpetualTaskParams.newBuilder().setCustomizedParams(Any.pack(ecsPerpetualTaskParams)).build();
    PerpetualTaskId perpetualTaskId = PerpetualTaskId.newBuilder().setId(PERPETUAL_TASK_ID).build();
    ecsPerpetualTaskExecutor.runOnce(perpetualTaskId, params, heartBeatTime);

    Instant now = Instant.now();
    then(ecsMetricClient)
        .should(times(1))
        .getUtilizationMetrics(eq(awsConfig), eq(encryptionDetails),
            eq(Date.from(now.truncatedTo(HOURS).minus(Duration.ofHours(24)))), eq(Date.from(now.truncatedTo(HOURS))),
            any(Cluster.class), anyListOf(Service.class), any(EcsPerpetualTaskParams.class));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHandleMultiHourWindowIfMissingLessThan24Hours() throws Exception {
    Instant now = Instant.now();
    Instant heartBeatTime = now.minus(Duration.ofHours(7));
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).build();
    ByteString bytes = ByteString.copyFrom(KryoUtils.asBytes(awsConfig));
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    List<EncryptedDataDetail> encryptionDetails = new ArrayList<>(Collections.singletonList(encryptedDataDetail));
    ByteString encryptionDetailBytes = ByteString.copyFrom(KryoUtils.asBytes(encryptionDetails));
    EcsPerpetualTaskParams ecsPerpetualTaskParams = EcsPerpetualTaskParams.newBuilder()
                                                        .setClusterId(CLUSTER_ID)
                                                        .setRegion(REGION)
                                                        .setSettingId(SETTING_ID)
                                                        .setClusterName(CLUSTER_NAME)
                                                        .setAwsConfig(bytes)
                                                        .setEncryptionDetail(encryptionDetailBytes)
                                                        .build();

    PerpetualTaskParams params =
        PerpetualTaskParams.newBuilder().setCustomizedParams(Any.pack(ecsPerpetualTaskParams)).build();
    PerpetualTaskId perpetualTaskId = PerpetualTaskId.newBuilder().setId(PERPETUAL_TASK_ID).build();
    ecsPerpetualTaskExecutor.runOnce(perpetualTaskId, params, heartBeatTime);

    then(ecsMetricClient)
        .should(times(1))
        .getUtilizationMetrics(eq(awsConfig), eq(encryptionDetails), eq(Date.from(heartBeatTime.truncatedTo(HOURS))),
            eq(Date.from(now.truncatedTo(HOURS))), any(Cluster.class), anyListOf(Service.class),
            any(EcsPerpetualTaskParams.class));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldNotPublishUtilizationMetricsIfAlreadyWithinLastHour() throws Exception {
    Instant now = Instant.now();
    Instant heartBeatTime = now.minus(Duration.ofHours(7));
    // lastMetricCollectionTime within last hour. So don't collect again.
    Reflect.on(ecsPerpetualTaskExecutor).set("lastMetricCollectionTime", now.minus(50, MINUTES));
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).build();
    ByteString bytes = ByteString.copyFrom(KryoUtils.asBytes(awsConfig));
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    List<EncryptedDataDetail> encryptionDetails = new ArrayList<>(Collections.singletonList(encryptedDataDetail));
    ByteString encryptionDetailBytes = ByteString.copyFrom(KryoUtils.asBytes(encryptionDetails));
    EcsPerpetualTaskParams ecsPerpetualTaskParams = EcsPerpetualTaskParams.newBuilder()
                                                        .setClusterId(CLUSTER_ID)
                                                        .setRegion(REGION)
                                                        .setSettingId(SETTING_ID)
                                                        .setClusterName(CLUSTER_NAME)
                                                        .setAwsConfig(bytes)
                                                        .setEncryptionDetail(encryptionDetailBytes)
                                                        .build();

    PerpetualTaskParams params =
        PerpetualTaskParams.newBuilder().setCustomizedParams(Any.pack(ecsPerpetualTaskParams)).build();
    PerpetualTaskId perpetualTaskId = PerpetualTaskId.newBuilder().setId(PERPETUAL_TASK_ID).build();
    ecsPerpetualTaskExecutor.runOnce(perpetualTaskId, params, heartBeatTime);

    then(ecsMetricClient)
        .should(never())
        .getUtilizationMetrics(any(AwsConfig.class), anyListOf(EncryptedDataDetail.class), any(Date.class),
            any(Date.class), any(Cluster.class), anyListOf(Service.class), any(EcsPerpetualTaskParams.class));
  }
}