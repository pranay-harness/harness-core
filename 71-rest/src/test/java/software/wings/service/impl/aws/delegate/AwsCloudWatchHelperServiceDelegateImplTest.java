package software.wings.service.impl.aws.delegate;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricDataResult;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.MetricDataResult;
import io.harness.CategoryTest;
import io.harness.aws.AwsCallTracker;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.OwnerRule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.request.AwsCloudWatchMetricDataRequest;
import software.wings.service.impl.aws.model.request.AwsCloudWatchStatisticsRequest;
import software.wings.service.impl.aws.model.response.AwsCloudWatchMetricDataResponse;
import software.wings.service.impl.aws.model.response.AwsCloudWatchStatisticsResponse;
import software.wings.service.intfc.security.EncryptionService;

public class AwsCloudWatchHelperServiceDelegateImplTest extends CategoryTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private AwsCallTracker mockTracker;

  @Spy @InjectMocks private AwsCloudWatchHelperServiceDelegateImpl awsCloudWatchHelperServiceDelegate;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Category(UnitTests.class)
  public void getMetricStatistics() {
    doReturn(null).when(mockEncryptionService).decrypt(any(AwsConfig.class), anyListOf(EncryptedDataDetail.class));
    final AmazonCloudWatchClient amazonCloudWatchClientMock = mock(AmazonCloudWatchClient.class);
    doReturn(amazonCloudWatchClientMock)
        .when(awsCloudWatchHelperServiceDelegate)
        .getAwsCloudWatchClient(anyString(), any(AwsConfig.class));
    doNothing().when(mockTracker).trackCloudWatchCall(anyString());

    final GetMetricStatisticsResult getMetricStatisticsResult = new GetMetricStatisticsResult();
    final Datapoint datapoint = new Datapoint();
    getMetricStatisticsResult.withDatapoints(datapoint);
    doReturn(getMetricStatisticsResult)
        .when(amazonCloudWatchClientMock)
        .getMetricStatistics(any(GetMetricStatisticsRequest.class));

    final AwsCloudWatchStatisticsRequest awsCloudWatchStatisticsRequest =
        AwsCloudWatchStatisticsRequest.builder().build();

    final AwsCloudWatchStatisticsResponse metricStatistics =
        awsCloudWatchHelperServiceDelegate.getMetricStatistics(awsCloudWatchStatisticsRequest);

    assertThat(metricStatistics.getDatapoints().get(0)).isEqualTo(datapoint);
  }

  @Test
  @Category(UnitTests.class)
  public void getMetricStatistics_error() {
    doReturn(null).when(mockEncryptionService).decrypt(any(AwsConfig.class), anyListOf(EncryptedDataDetail.class));
    final AmazonCloudWatchClient amazonCloudWatchClientMock = mock(AmazonCloudWatchClient.class);
    doReturn(amazonCloudWatchClientMock)
        .when(awsCloudWatchHelperServiceDelegate)
        .getAwsCloudWatchClient(anyString(), any(AwsConfig.class));
    doNothing().when(mockTracker).trackCloudWatchCall(anyString());

    doThrow(new AmazonServiceException(""))
        .when(amazonCloudWatchClientMock)
        .getMetricStatistics(any(GetMetricStatisticsRequest.class));

    final AwsCloudWatchStatisticsRequest awsCloudWatchStatisticsRequest =
        AwsCloudWatchStatisticsRequest.builder().build();

    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> awsCloudWatchHelperServiceDelegate.getMetricStatistics(awsCloudWatchStatisticsRequest));

    doThrow(new AmazonClientException(""))
        .when(amazonCloudWatchClientMock)
        .getMetricStatistics(any(GetMetricStatisticsRequest.class));

    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> awsCloudWatchHelperServiceDelegate.getMetricStatistics(awsCloudWatchStatisticsRequest));
  }

  @Test
  @Owner(emails = AVMOHAN, resent = false)
  @Category(UnitTests.class)
  public void shouldGetMetricData() throws Exception {
    final AmazonCloudWatchClient amazonCloudWatchClientMock = mock(AmazonCloudWatchClient.class);
    final AwsConfig awsConfig = AwsConfig.builder().build();
    doReturn(amazonCloudWatchClientMock)
        .when(awsCloudWatchHelperServiceDelegate)
        .getAwsCloudWatchClient(eq("REGION"), refEq(awsConfig));

    final MetricDataResult dataResult = new MetricDataResult();
    final GetMetricDataResult getMetricDataResult = new GetMetricDataResult().withMetricDataResults(dataResult);
    //    getMetricDataResult.withDatapoints(datapoint);
    doReturn(getMetricDataResult).when(amazonCloudWatchClientMock).getMetricData(any(GetMetricDataRequest.class));

    final AwsCloudWatchMetricDataRequest awsCloudWatchMetricDataRequest =
        AwsCloudWatchMetricDataRequest.builder().awsConfig(awsConfig).region("REGION").build();

    final AwsCloudWatchMetricDataResponse metricDataResponse =
        awsCloudWatchHelperServiceDelegate.getMetricData(awsCloudWatchMetricDataRequest);
    assertThat(metricDataResponse.getMetricDataResults()).hasSize(1).containsExactly(dataResult);
  }

  @Test
  @Owner(emails = AVMOHAN, resent = false)
  @Category(UnitTests.class)
  public void shouldHandleAmazonServiceExceptionWhenGetMetricData() {
    final AmazonCloudWatchClient amazonCloudWatchClientMock = mock(AmazonCloudWatchClient.class);
    doReturn(amazonCloudWatchClientMock)
        .when(awsCloudWatchHelperServiceDelegate)
        .getAwsCloudWatchClient(anyString(), any(AwsConfig.class));

    final AwsCloudWatchMetricDataRequest awsCloudWatchMetricDataRequest =
        AwsCloudWatchMetricDataRequest.builder().build();

    doThrow(new AmazonServiceException(""))
        .when(amazonCloudWatchClientMock)
        .getMetricData(any(GetMetricDataRequest.class));

    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> awsCloudWatchHelperServiceDelegate.getMetricData(awsCloudWatchMetricDataRequest));
  }

  @Test
  @Owner(emails = AVMOHAN, resent = false)
  @Category(UnitTests.class)
  public void shouldHandleAmazonClientExceptionWhenGetMetricData() {
    final AmazonCloudWatchClient amazonCloudWatchClientMock = mock(AmazonCloudWatchClient.class);
    doReturn(amazonCloudWatchClientMock)
        .when(awsCloudWatchHelperServiceDelegate)
        .getAwsCloudWatchClient(anyString(), any(AwsConfig.class));

    final AwsCloudWatchMetricDataRequest awsCloudWatchMetricDataRequest =
        AwsCloudWatchMetricDataRequest.builder().build();

    doThrow(new AmazonClientException(""))
        .when(amazonCloudWatchClientMock)
        .getMetricData(any(GetMetricDataRequest.class));

    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> awsCloudWatchHelperServiceDelegate.getMetricData(awsCloudWatchMetricDataRequest));
  }
}