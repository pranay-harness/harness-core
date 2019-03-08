package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

import com.google.inject.Inject;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import org.junit.Before;
import org.junit.Test;
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
  public void shouldListNamespaces() {
    List<String> namespaces = cloudWatchService.listNamespaces(SETTING_ID, "us-east-1");
    assertThat(namespaces).hasSize(1).containsExactly(NAMESPACE);
  }

  @Test
  public void shouldListMetrics() {
    List<String> namespaces = cloudWatchService.listMetrics(SETTING_ID, "us-east-1", NAMESPACE);
    assertThat(namespaces).hasSize(1).containsExactly(METRIC_NAME);
  }

  @Test
  public void shouldListDimensions() {
    List<String> namespaces = cloudWatchService.listDimensions(SETTING_ID, "us-east-1", NAMESPACE, METRIC_NAME);
    assertThat(namespaces).hasSize(1).containsExactly(METRIC_DIMENSION);
  }

  @Test
  public void testFetchMetricsAllMetrics() {
    Map<AwsNameSpace, List<CloudWatchMetric>> cloudwatchMetrics = CloudWatchServiceImpl.fetchMetrics();
    assertEquals("There are 4 different types of metrics", 4, cloudwatchMetrics.keySet().size());
    assertEquals("There are 4 lambda metrics in the list", 4, cloudwatchMetrics.get(AwsNameSpace.LAMBDA).size());
    assertEquals("There are 4 ECS metrics in the list", 4, cloudwatchMetrics.get(AwsNameSpace.ECS).size());
    assertEquals("There are 9 EC2 metrics in the list", 9, cloudwatchMetrics.get(AwsNameSpace.EC2).size());
    assertEquals("There are 10 ELB metrics in the list", 10, cloudwatchMetrics.get(AwsNameSpace.ELB).size());
  }

  @Test
  public void testFetchSpecificMetrics() {
    CloudWatchCVServiceConfiguration cvServiceConfiguration =
        CloudWatchCVServiceConfiguration.builder().clusterName("testCluster").build();
    Map<String, List<CloudWatchMetric>> elbList = new HashMap<>();
    elbList.put("testLB", Arrays.asList(CloudWatchMetric.builder().metricName("HTTP200x").build()));
    cvServiceConfiguration.setLoadBalancerMetrics(elbList);

    Map<AwsNameSpace, List<CloudWatchMetric>> cloudwatchMetrics =
        CloudWatchServiceImpl.fetchMetrics(cvServiceConfiguration);
    assertEquals("There are 2 different types of metrics", 2, cloudwatchMetrics.keySet().size());
    assertFalse("There are no lambda metrics", cloudwatchMetrics.containsKey(AwsNameSpace.LAMBDA));
    assertFalse("There are no EC2 metrics", cloudwatchMetrics.containsKey(AwsNameSpace.EC2));
    assertEquals("There are 4 ECS metrics in the list", 4, cloudwatchMetrics.get(AwsNameSpace.ECS).size());
    assertEquals("There are 10 ELB metrics in the list", 10, cloudwatchMetrics.get(AwsNameSpace.ELB).size());
  }

  @Test
  public void testFetchSpecificMetricsNone() {
    CloudWatchCVServiceConfiguration cvServiceConfiguration = CloudWatchCVServiceConfiguration.builder().build();

    Map<AwsNameSpace, List<CloudWatchMetric>> cloudwatchMetrics =
        CloudWatchServiceImpl.fetchMetrics(cvServiceConfiguration);
    assertEquals("There are 0 different types of metrics", 0, cloudwatchMetrics.keySet().size());
    assertFalse("There are no lambda metrics", cloudwatchMetrics.containsKey(AwsNameSpace.LAMBDA));
    assertFalse("There are no EC2 metrics", cloudwatchMetrics.containsKey(AwsNameSpace.EC2));
    assertFalse("There are no ECS metrics", cloudwatchMetrics.containsKey(AwsNameSpace.ECS));
    assertFalse("There are no ELB metrics", cloudwatchMetrics.containsKey(AwsNameSpace.ELB));
  }
}
