package software.wings.sm.states;

import static com.amazonaws.services.cloudwatch.model.Statistic.Average;
import static com.amazonaws.services.cloudwatch.model.Statistic.Maximum;
import static com.amazonaws.services.cloudwatch.model.Statistic.Minimum;
import static com.amazonaws.services.cloudwatch.model.Statistic.SampleCount;
import static com.amazonaws.services.cloudwatch.model.Statistic.Sum;
import static java.util.Arrays.asList;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by anubhaw on 12/6/16.
 */
public class CloudWatchStateTest {
  //  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-ddThh:mm:ss"); // 2014-09-03T23:00:00Z

  @Test
  @Ignore
  public void shouldFetchMetrics() {
    BasicAWSCredentials awsCredentials =
        new BasicAWSCredentials("AKIAI6IK4KYQQQEEWEVA", "a0j7DacqjfQrjMwIIWgERrbxsuN5cyivdNhyo6wy");

    AmazonCloudWatchClient cloudWatchClient = new AmazonCloudWatchClient(awsCredentials);

    ListMetricsResult listMetricsResult = cloudWatchClient.listMetrics();

    GetMetricStatisticsRequest getMetricRequest = new GetMetricStatisticsRequest();

    getMetricRequest.setNamespace("AWS/EC2");
    getMetricRequest.setMetricName("CPUUtilization");

    long currentTimeMillis = System.currentTimeMillis();

    getMetricRequest.setStartTime(new Date(currentTimeMillis - 10 * 10 * 60 * 1000));
    getMetricRequest.setEndTime(new Date(currentTimeMillis));
    getMetricRequest.setPeriod(10 * 60);
    getMetricRequest.setStatistics(
        asList(SampleCount.name(), Average.name(), Sum.name(), Minimum.name(), Maximum.name()));
    List<Dimension> dimensions = new ArrayList<>();
    //    dimensions.add(new Dimension().withName("InstanceId").withValue("i-ba17cb25"));
    getMetricRequest.setDimensions(dimensions);

    GetMetricStatisticsResult metricStatistics = cloudWatchClient.getMetricStatistics(getMetricRequest);
    Datapoint datapoint =
        metricStatistics.getDatapoints().stream().min(Comparator.comparing(Datapoint::getTimestamp)).get();
    List<Datapoint> dps = new ArrayList<>();
    Datapoint datapoint1 = dps.stream().min(Comparator.comparing(Datapoint::getTimestamp)).orElse(null);

    System.out.println(metricStatistics);
  }
}
