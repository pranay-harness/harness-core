package software.wings.service;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.AwsConfig.Builder.anAwsConfig;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.METRIC_DIMENSION;
import static software.wings.utils.WingsTestConstants.METRIC_NAME;
import static software.wings.utils.WingsTestConstants.NAMESPACE;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.CloudWatchService;
import software.wings.service.intfc.SettingsService;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 12/15/16.
 */
public class CloudWatchServiceTest extends WingsBaseTest {
  @Mock private SettingsService settingsService;
  @Mock private AwsHelperService awsHelperService;

  @Mock private AmazonCloudWatchClient amazonCloudWatchClient;

  @Inject @InjectMocks private CloudWatchService cloudWatchService;

  @Before
  public void setUp() throws Exception {
    when(settingsService.get(SETTING_ID))
        .thenReturn(
            aSettingAttribute()
                .withValue(
                    anAwsConfig().withAccessKey(ACCESS_KEY).withSecretKey(SECRET_KEY).withAccountId(ACCOUNT_ID).build())
                .build());
    when(awsHelperService.getAwsCloudWatchClient(ACCESS_KEY, SECRET_KEY)).thenReturn(amazonCloudWatchClient);

    ListMetricsResult listMetricsResult = new ListMetricsResult().withMetrics(
        asList(new Metric()
                   .withNamespace(NAMESPACE)
                   .withMetricName(METRIC_NAME)
                   .withDimensions(asList(new Dimension().withName(METRIC_DIMENSION)))));
    when(amazonCloudWatchClient.listMetrics()).thenReturn(listMetricsResult);
    when(amazonCloudWatchClient.listMetrics(any(ListMetricsRequest.class))).thenReturn(listMetricsResult);
  }

  @Test
  public void shouldListNamespaces() {
    List<String> namespaces = cloudWatchService.listNamespaces(SETTING_ID);
    Assertions.assertThat(namespaces).hasSize(1).containsExactly(NAMESPACE);
  }

  @Test
  public void shouldListMetrics() {
    List<String> namespaces = cloudWatchService.listMetrics(SETTING_ID, NAMESPACE);
    Assertions.assertThat(namespaces).hasSize(1).containsExactly(METRIC_NAME);
  }

  @Test
  public void shouldListDimensions() {
    List<String> namespaces = cloudWatchService.listDimensions(SETTING_ID, NAMESPACE, METRIC_NAME);
    Assertions.assertThat(namespaces).hasSize(1).containsExactly(METRIC_DIMENSION);
  }
}
