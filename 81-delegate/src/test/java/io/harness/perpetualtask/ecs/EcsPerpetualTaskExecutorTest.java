package io.harness.perpetualtask.ecs;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.HITESH;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.eq;
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
import io.harness.rule.OwnerRule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class EcsPerpetualTaskExecutorTest extends CategoryTest {
  @Mock private AwsEcsHelperServiceDelegate ecsHelperServiceDelegate;
  @Mock private AwsEc2HelperServiceDelegate ec2ServiceDelegate;
  @Mock private EcsMetricClient ecsMetricClient;
  @Mock private EventPublisher eventPublisher;
  private EcsPerpetualTaskExecutor ecsPerpetualTaskExecutor;

  @Captor private ArgumentCaptor<Message> messageCaptor;

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
    Instant pollTime = Instant.now();
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
    given(ecsMetricClient.getUtilizationMetrics(awsConfig, encryptionDetails, Date.from(pollTime.minus(10, MINUTES)),
              Date.from(pollTime), new Cluster().withClusterName(CLUSTER_NAME).withClusterArn(CLUSTER_ARN), services,
              EcsPerpetualTaskParams.newBuilder()
                  .setClusterId(CLUSTER_ID)
                  .setSettingId(SETTING_ID)
                  .setRegion(REGION)
                  .build()))
        .willReturn(utilizationMessages);

    ecsPerpetualTaskExecutor.publishUtilizationMetrics(
        CLUSTER_ID, SETTING_ID, REGION, awsConfig, encryptionDetails, CLUSTER_ARN, pollTime);

    then(eventPublisher)
        .should(times(2))
        .publishMessage(messageCaptor.capture(), eq(HTimestamps.fromInstant(pollTime)));
    assertThat(messageCaptor.getAllValues()).hasSize(2).containsAll(utilizationMessages);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldQueryEcsClientAndPublishSyncMessages() {
    Set<String> activeEc2InstanceIds = new HashSet<>(Arrays.asList("instance1", "instance2"));
    Set<String> activeContainerInstanceArns = new HashSet<>(Arrays.asList("containerInstance1"));
    Set<String> activeTaskArns = new HashSet<>(Arrays.asList("task1", "task2"));
    Instant pollTime = Instant.now();

    ecsPerpetualTaskExecutor.publishEcsClusterSyncEvent(CLUSTER_ID, SETTING_ID, CLUSTER_NAME, activeEc2InstanceIds,
        activeContainerInstanceArns, activeTaskArns, pollTime);
    then(eventPublisher)
        .should(times(1))
        .publishMessage(messageCaptor.capture(), eq(HTimestamps.fromInstant(pollTime)));
    assertThat(messageCaptor.getAllValues()).hasSize(1);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldRunEcsPerpetualTask() {
    Instant heartBeatTime = Instant.now();

    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).build();
    ByteString bytes = ByteString.copyFrom(KryoUtils.asBytes(awsConfig));

    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    List<EncryptedDataDetail> encryptionDetails = new ArrayList<>(Arrays.asList(encryptedDataDetail));
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
}