package software.wings.sm.states;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
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
    GetMetricStatisticsRequest getMetricRequest = new GetMetricStatisticsRequest();

    getMetricRequest.setNamespace("AWS/EC2");
    getMetricRequest.setMetricName("CPUUtilization");

    long currentTimeMillis = System.currentTimeMillis();

    getMetricRequest.setStartTime(new Date(currentTimeMillis - 30 * 60 * 1000));
    getMetricRequest.setEndTime(new Date(currentTimeMillis));
    getMetricRequest.setPeriod(24 * 60 * 60);
    List<String> stats = new ArrayList<>();
    stats.add("Average");
    getMetricRequest.setStatistics(stats);
    List<Dimension> dimensions = new ArrayList<>();
    dimensions.add(new Dimension().withName("InstanceId").withValue("i-ba17cb25"));
    getMetricRequest.setDimensions(dimensions);

    GetMetricStatisticsResult metricStatistics = cloudWatchClient.getMetricStatistics(getMetricRequest);
    System.out.println(metricStatistics);
  }
}
