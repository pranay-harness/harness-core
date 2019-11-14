package io.harness.perpetualtask.ecs;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;

import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.Service;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.client.EventPublisher;
import io.harness.event.payloads.EcsUtilization;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.ecs.support.EcsMetricClient;
import io.harness.rule.OwnerRule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
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
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class EcsPerpetualTaskExecutorTest extends CategoryTest {
  @Mock private AwsEcsHelperServiceDelegate ecsHelperServiceDelegate;
  @Mock private AwsEc2HelperServiceDelegate ec2ServiceDelegate;
  @Mock private EcsMetricClient ecsMetricClient;
  @Mock private EventPublisher eventPublisher;
  private EcsPerpetualTaskExecutor ecsPerpetualTaskExecutor;

  @Captor private ArgumentCaptor<Message> messageCaptor;

  @Before
  public void setUp() throws Exception {
    ecsPerpetualTaskExecutor =
        new EcsPerpetualTaskExecutor(ecsHelperServiceDelegate, ec2ServiceDelegate, ecsMetricClient, eventPublisher);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldQueryEcsMetricClientAndPublishUtilizationMessages() throws Exception {
    String region = "us-east-1";
    AwsConfig awsConfig = AwsConfig.builder().build();
    List<EncryptedDataDetail> encryptionDetails = Collections.emptyList();
    String clusterArn = "arn:aws:ecs:us-east-2:132359207506:cluster/ecs-ccm-cluster";
    String clusterName = "ecs-ccm-cluster";
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

    given(ecsHelperServiceDelegate.listServicesForCluster(awsConfig, encryptionDetails, region, clusterArn))
        .willReturn(services);
    given(ecsMetricClient.getUtilizationMetrics(awsConfig, encryptionDetails, region,
              Date.from(pollTime.minus(10, MINUTES)), Date.from(pollTime),
              new Cluster().withClusterName(clusterName).withClusterArn(clusterArn), services))
        .willReturn(utilizationMessages);

    ecsPerpetualTaskExecutor.publishUtilizationMetrics(region, awsConfig, encryptionDetails, clusterArn, pollTime);

    then(eventPublisher)
        .should(times(2))
        .publishMessage(messageCaptor.capture(), eq(HTimestamps.fromInstant(pollTime)));
    assertThat(messageCaptor.getAllValues()).hasSize(2).containsAll(utilizationMessages);
  }
}