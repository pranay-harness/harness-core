package io.harness.perpetualtask.ecs.support;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;

import com.amazonaws.services.cloudwatch.model.MetricDataResult;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.Service;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.payloads.EcsUtilization;
import io.harness.event.payloads.EcsUtilization.MetricValue;
import io.harness.grpc.utils.HTimestamps;
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
import software.wings.service.impl.aws.model.request.AwsCloudWatchMetricDataRequest;
import software.wings.service.impl.aws.model.response.AwsCloudWatchMetricDataResponse;
import software.wings.service.intfc.aws.delegate.AwsCloudWatchHelperServiceDelegate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class EcsMetricClientTest extends CategoryTest {
  @Mock private AwsCloudWatchHelperServiceDelegate awsCloudWatchHelperServiceDelegate;
  @Captor private ArgumentCaptor<AwsCloudWatchMetricDataRequest> captor;
  private EcsMetricClient ecsMetricClient;

  @Before
  public void setUp() throws Exception {
    ecsMetricClient = new EcsMetricClient(awsCloudWatchHelperServiceDelegate);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldMakeAggregatedQuery() throws Exception {
    AwsConfig awsConfig = AwsConfig.builder().build();
    List<EncryptedDataDetail> encryptionDetails = emptyList();
    String region = "region";
    Instant now = Instant.now();
    Date startTime = Date.from(now.minus(10, ChronoUnit.MINUTES));
    Date endTime = Date.from(now);
    Cluster cluster = new Cluster().withClusterName("cluster1").withClusterArn("cluster1-arn");
    Service svc1 = new Service().withServiceName("svc1").withServiceArn("svc1-arn");
    Service svc2 = new Service().withServiceName("svc2").withServiceArn("svc2-arn");
    List<Service> services = Arrays.asList(svc1, svc2);

    // stubbing limited results only, enough to cover the paths
    given(awsCloudWatchHelperServiceDelegate.getMetricData(any(AwsCloudWatchMetricDataRequest.class)))
        .willReturn(AwsCloudWatchMetricDataResponse.builder()
                        .metricDataResults(Arrays.asList(new MetricDataResult()
                                                             .withId("id_40d5949f5260813fd3f88c99a3bc0170")
                                                             .withTimestamps(singletonList(startTime))
                                                             .withValues(singletonList(33.4)),
                            new MetricDataResult()
                                .withId("id_178377ef7b82da38dd017df94baaf4f0")
                                .withTimestamps(singletonList(startTime))
                                .withValues(singletonList(78.3)),
                            new MetricDataResult()
                                .withId("id_60c65ecf8d7a5099a846ceb2bfcfeb8f")
                                .withTimestamps(singletonList(endTime))
                                .withValues(singletonList(66.32))))
                        .build());

    final List<EcsUtilization> utilizationMetrics = ecsMetricClient.getUtilizationMetrics(
        awsConfig, encryptionDetails, region, startTime, endTime, cluster, services);
    assertThat(utilizationMetrics)
        .containsExactlyInAnyOrder(
            EcsUtilization.newBuilder()
                .setClusterArn("cluster1-arn")
                .setClusterName("cluster1")
                .addMetricValues(MetricValue.newBuilder()
                                     .setMetricName("CPUUtilization")
                                     .setStatistic("Average")
                                     .addAllTimestamps(singletonList(HTimestamps.fromDate(startTime)))
                                     .addAllValues(singletonList(78.3)))
                .build(),

            EcsUtilization.newBuilder()
                .setClusterArn("cluster1-arn")
                .setClusterName("cluster1")
                .setServiceArn("svc2-arn")
                .setServiceName("svc2")
                .addMetricValues(MetricValue.newBuilder()
                                     .setMetricName("CPUUtilization")
                                     .setStatistic("Maximum")
                                     .addAllTimestamps(singletonList(HTimestamps.fromDate(endTime)))
                                     .addAllValues(singletonList(66.32)))
                .build(),

            EcsUtilization.newBuilder()
                .setClusterArn("cluster1-arn")
                .setClusterName("cluster1")
                .setServiceArn("svc1-arn")
                .setServiceName("svc1")
                .addMetricValues(MetricValue.newBuilder()
                                     .setMetricName("MemoryUtilization")
                                     .setStatistic("Average")
                                     .addAllTimestamps(singletonList(HTimestamps.fromDate(startTime)))
                                     .addAllValues(singletonList(33.4)))
                .build()

        );

    then(awsCloudWatchHelperServiceDelegate).should().getMetricData(captor.capture());
    AwsCloudWatchMetricDataRequest request = captor.getValue();
    assertThat(request.getStartTime()).isEqualTo(startTime);
    assertThat(request.getEndTime()).isEqualTo(endTime);
    assertThat(request.getRegion()).isEqualTo(region);
    // #(metrics)*#(stats)*#(clusters)*(1+#(services))
    assertThat(request.getMetricDataQueries()).hasSize(12);
  }
}