package software.wings.sm.states;

import static com.amazonaws.services.cloudwatch.model.Statistic.Average;
import static com.amazonaws.services.cloudwatch.model.Statistic.Maximum;
import static com.amazonaws.services.cloudwatch.model.Statistic.Minimum;
import static com.amazonaws.services.cloudwatch.model.Statistic.SampleCount;
import static com.amazonaws.services.cloudwatch.model.Statistic.Sum;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ASSERTION;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.METRIC_NAME;
import static software.wings.utils.WingsTestConstants.NAMESPACE;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.AwsConfig;
import software.wings.beans.Base;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by anubhaw on 12/6/16.
 */
public class CloudWatchStateTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(CloudWatchStateTest.class);

  private static final Activity ACTIVITY_WITH_ID = Activity.builder().build();

  static {
    ACTIVITY_WITH_ID.setUuid(ACTIVITY_ID);
  }

  public static final String PERCENTILE = "p90";
  @Mock private SettingsService settingsService;
  @Mock private ActivityService activityService;
  @Mock private AwsHelperService awsHelperService;

  @Mock private ExecutionContextImpl context;
  @Mock private AmazonCloudWatchClient amazonCloudWatchClient;
  @Mock private SecretManager secretManager;

  @InjectMocks private CloudWatchState cloudWatchState = new CloudWatchState(StateType.CLOUD_WATCH.name());

  @Before
  public void setUp() throws Exception {
    when(context.getApp()).thenReturn(anApplication().withUuid(APP_ID).build());
    when(context.getEnv()).thenReturn(anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).build());
    when(activityService.save(any(Activity.class))).thenReturn(ACTIVITY_WITH_ID);
    when(settingsService.get(Base.GLOBAL_APP_ID, SETTING_ID))
        .thenReturn(aSettingAttribute()
                        .withValue(AwsConfig.builder().accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build())
                        .build());
    when(secretManager.getEncryptionDetails(anyObject(), anyString(), anyString())).thenReturn(Collections.emptyList());
    setInternalState(cloudWatchState, "secretManager", secretManager);
  }

  @Test
  public void shouldExecute() {
    when(context.evaluateExpression(eq(ASSERTION), any(Datapoint.class))).thenReturn(true);
    ArgumentCaptor<GetMetricStatisticsRequest> argumentCaptor =
        ArgumentCaptor.forClass(GetMetricStatisticsRequest.class);
    when(awsHelperService.getCloudWatchMetricStatistics(
             any(AwsConfig.class), any(), anyString(), argumentCaptor.capture()))
        .thenReturn(new Datapoint());

    cloudWatchState.setAwsCredentialsConfigId(SETTING_ID);
    cloudWatchState.setAssertion(ASSERTION);
    cloudWatchState.setNamespace(NAMESPACE);
    cloudWatchState.setMetricName(METRIC_NAME);
    cloudWatchState.setPercentile(PERCENTILE);

    cloudWatchState.execute(context);

    verify(activityService).save(any(Activity.class));
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
    GetMetricStatisticsRequest getMetricStatisticsRequest = argumentCaptor.getValue();
    assertThat(getMetricStatisticsRequest.getNamespace()).isEqualTo(NAMESPACE);
    assertThat(getMetricStatisticsRequest.getMetricName()).isEqualTo(METRIC_NAME);
    assertThat(getMetricStatisticsRequest.getStatistics())
        .containsExactlyElementsOf(
            asList(SampleCount.name(), Average.name(), Sum.name(), Minimum.name(), Maximum.name()));
    assertThat(getMetricStatisticsRequest.getExtendedStatistics()).containsExactly(PERCENTILE);
  }

  @Test
  @Ignore
  public void shouldFetchMetrics() {
    BasicAWSCredentials awsCredentials =
        new BasicAWSCredentials("AKIAI6IK4KYQQQEEWEVA", "a0j7DacqjfQrjMwIIWgERrbxsuN5cyivdNhyo6wy");

    AmazonCloudWatchClient cloudWatchClient = new AmazonCloudWatchClient(awsCredentials);

    cloudWatchClient.listMetrics();

    GetMetricStatisticsRequest getMetricRequest = new GetMetricStatisticsRequest();

    getMetricRequest.setNamespace("AWS/EC2");
    getMetricRequest.setMetricName("CPUUtilization");

    long currentTimeMillis = System.currentTimeMillis();

    getMetricRequest.setStartTime(new Date(currentTimeMillis - 10 * 10 * 60 * 1000));
    getMetricRequest.setEndTime(new Date(currentTimeMillis));
    getMetricRequest.setPeriod(10 * 60);
    getMetricRequest.setStatistics(
        asList(SampleCount.name(), Average.name(), Sum.name(), Minimum.name(), Maximum.name()));
    getMetricRequest.setExtendedStatistics(asList());
    List<Dimension> dimensions = new ArrayList<>();
    getMetricRequest.setDimensions(dimensions);

    GetMetricStatisticsResult metricStatistics = cloudWatchClient.getMetricStatistics(getMetricRequest);
    Datapoint datapoint =
        metricStatistics.getDatapoints().stream().min(Comparator.comparing(Datapoint::getTimestamp)).get();
    List<Datapoint> dps = new ArrayList<>();
    Datapoint datapoint1 = dps.stream().min(Comparator.comparing(Datapoint::getTimestamp)).orElse(null);

    logger.info(metricStatistics.toString());
  }
}
