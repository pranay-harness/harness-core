package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.METRIC_DIMENSION;
import static software.wings.utils.WingsTestConstants.METRIC_NAME;
import static software.wings.utils.WingsTestConstants.NAMESPACE;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.CloudWatchServiceImpl;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.intfc.CloudWatchService;
import software.wings.service.intfc.SettingsService;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 12/15/16.
 */
public class CloudWatchServiceTest extends WingsBaseTest {
  @Mock private SettingsService settingsService;
  @Mock private AwsHelperService awsHelperService;

  @Inject @InjectMocks private CloudWatchService cloudWatchService;

  @Before
  public void setUp() throws Exception {
    when(settingsService.get(SETTING_ID))
        .thenReturn(aSettingAttribute()
                        .withValue(AwsConfig.builder().accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build())
                        .build());
    ListMetricsResult listMetricsResult = new ListMetricsResult().withMetrics(
        asList(new Metric()
                   .withNamespace(NAMESPACE)
                   .withMetricName(METRIC_NAME)
                   .withDimensions(asList(new Dimension().withName(METRIC_DIMENSION)))));
    when(awsHelperService.getCloudWatchMetrics(any(AwsConfig.class), any(), anyString()))
        .thenReturn(listMetricsResult.getMetrics());
    when(awsHelperService.getCloudWatchMetrics(any(AwsConfig.class), any(), anyString(), any(ListMetricsRequest.class)))
        .thenReturn(listMetricsResult.getMetrics());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldListNamespaces() {
    List<String> namespaces = cloudWatchService.listNamespaces(SETTING_ID, "us-east-1");
    assertThat(namespaces).hasSize(1).containsExactly(NAMESPACE);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldListMetrics() {
    List<String> namespaces = cloudWatchService.listMetrics(SETTING_ID, "us-east-1", NAMESPACE);
    assertThat(namespaces).hasSize(1).containsExactly(METRIC_NAME);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldListDimensions() {
    List<String> namespaces = cloudWatchService.listDimensions(SETTING_ID, "us-east-1", NAMESPACE, METRIC_NAME);
    assertThat(namespaces).hasSize(1).containsExactly(METRIC_DIMENSION);
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchMetricsAllMetrics() {
    Map<AwsNameSpace, List<CloudWatchMetric>> cloudwatchMetrics = CloudWatchServiceImpl.fetchMetrics();
    assertThat(cloudwatchMetrics.keySet()).hasSize(4);
    assertThat(cloudwatchMetrics.get(AwsNameSpace.LAMBDA)).hasSize(4);
    assertThat(cloudwatchMetrics.get(AwsNameSpace.ECS)).hasSize(4);
    assertThat(cloudwatchMetrics.get(AwsNameSpace.EC2)).hasSize(9);
    assertThat(cloudwatchMetrics.get(AwsNameSpace.ELB)).hasSize(13);
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchSpecificMetrics() {
    CloudWatchCVServiceConfiguration cvServiceConfiguration = CloudWatchCVServiceConfiguration.builder().build();
    Map<String, List<CloudWatchMetric>> ecsList = new HashMap<>();
    ecsList.put("testCluster", Arrays.asList(CloudWatchMetric.builder().metricName("CPUUtilization").build()));
    cvServiceConfiguration.setEcsMetrics(ecsList);
    Map<String, List<CloudWatchMetric>> elbList = new HashMap<>();
    elbList.put("testLB", Arrays.asList(CloudWatchMetric.builder().metricName("HTTPCode_Backend_2XX").build()));
    cvServiceConfiguration.setLoadBalancerMetrics(elbList);

    Map<AwsNameSpace, List<CloudWatchMetric>> cloudwatchMetrics =
        CloudWatchServiceImpl.fetchMetrics(cvServiceConfiguration);
    assertThat(cloudwatchMetrics.keySet()).hasSize(2);
    assertThat(cloudwatchMetrics.containsKey(AwsNameSpace.LAMBDA)).isFalse();
    assertThat(cloudwatchMetrics.containsKey(AwsNameSpace.EC2)).isFalse();
    assertThat(cloudwatchMetrics.get(AwsNameSpace.ECS)).hasSize(1);
    assertEquals("CPUUtilization", cloudwatchMetrics.get(AwsNameSpace.ECS).get(0).getMetricName());
    assertThat(cloudwatchMetrics.get(AwsNameSpace.ELB)).hasSize(1);
    assertEquals("HTTPCode_Backend_2XX", cloudwatchMetrics.get(AwsNameSpace.ELB).get(0).getMetricName());
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchSpecificMetricsNone() {
    CloudWatchCVServiceConfiguration cvServiceConfiguration = CloudWatchCVServiceConfiguration.builder().build();

    Map<AwsNameSpace, List<CloudWatchMetric>> cloudwatchMetrics =
        CloudWatchServiceImpl.fetchMetrics(cvServiceConfiguration);
    assertThat(cloudwatchMetrics.keySet()).hasSize(0);
    assertThat(cloudwatchMetrics.containsKey(AwsNameSpace.LAMBDA)).isFalse();
    assertThat(cloudwatchMetrics.containsKey(AwsNameSpace.EC2)).isFalse();
    assertThat(cloudwatchMetrics.containsKey(AwsNameSpace.ECS)).isFalse();
    assertThat(cloudwatchMetrics.containsKey(AwsNameSpace.ELB)).isFalse();
  }

  @Test
  @Category(UnitTests.class)
  public void testSetStatisticsAndUnit() {
    List<CloudWatchMetric> cloudWatchMetrics = Lists.newArrayList(CloudWatchMetric.builder()
                                                                      .metricName("Latency")
                                                                      .metricType(generateUuid())
                                                                      .unit(StandardUnit.Milliseconds)
                                                                      .build(),
        CloudWatchMetric.builder().metricName("RequestCount").metricType(generateUuid()).statistics("custom").build());

    cloudWatchService.setStatisticsAndUnit(AwsNameSpace.ELB, cloudWatchMetrics);

    assertThat(cloudWatchMetrics.get(0).getStatistics()).isEqualTo("Average");
    assertThat(cloudWatchMetrics.get(0).getUnit()).isEqualTo(StandardUnit.Milliseconds);
    assertThat(cloudWatchMetrics.get(1).getStatistics()).isEqualTo("Sum");
    assertThat(cloudWatchMetrics.get(1).getUnit()).isEqualTo(StandardUnit.Count);
  }
}
