package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Pranjal on 05/25/2019
 */
public class CloudWatchStateTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void testValidateFieldsInvalidCase() {
    CloudWatchState cloudWatchState = new CloudWatchState("dummy");
    // not adding any metrics for verification
    Map<String, String> invalidFields = cloudWatchState.validateFields();
    assertTrue(isEmpty(cloudWatchState.fetchEcsMetrics()));
    assertTrue(isEmpty(cloudWatchState.fetchLoadBalancerMetrics()));
    assertTrue("Size should be 1", invalidFields.size() == 1);
    assertEquals("Metrics Missing", "No metrics provided", invalidFields.keySet().iterator().next());
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateFieldsValidCaseLambdaProvided() {
    CloudWatchState cloudWatchState = new CloudWatchState("dummy");
    cloudWatchState.setShouldDoLambdaVerification(true);
    Map<String, String> invalidFields = cloudWatchState.validateFields();
    assertTrue(isEmpty(cloudWatchState.fetchEcsMetrics()));
    assertTrue(isEmpty(cloudWatchState.fetchLoadBalancerMetrics()));
    assertTrue(isEmpty(cloudWatchState.fetchEc2Metrics()));
    assertTrue(cloudWatchState.isShouldDoLambdaVerification());
    assertFalse(cloudWatchState.isShouldDoECSClusterVerification());

    assertTrue("Size should be 0", invalidFields.size() == 0);
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateFieldsValidCaseECSProvided() {
    CloudWatchState cloudWatchState = new CloudWatchState("dummy");
    cloudWatchState.setShouldDoECSClusterVerification(true);
    Map<String, String> invalidFields = cloudWatchState.validateFields();
    assertTrue(isEmpty(cloudWatchState.fetchEcsMetrics()));
    assertTrue(isEmpty(cloudWatchState.fetchLoadBalancerMetrics()));
    assertTrue(isEmpty(cloudWatchState.fetchEc2Metrics()));
    assertTrue(cloudWatchState.isShouldDoECSClusterVerification());
    assertFalse(cloudWatchState.isShouldDoLambdaVerification());

    assertTrue("Size should be 0", invalidFields.size() == 0);
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateFieldsValidCaseLoadBalancerProvided() {
    CloudWatchState cloudWatchState = new CloudWatchState("dummy");
    Map<String, List<CloudWatchMetric>> metrics = new HashMap<>();
    List<CloudWatchMetric> metricList = new ArrayList<>();
    metricList.add(CloudWatchMetric.builder().metricName("asdf").build());
    metrics.put("loadbalancer", metricList);

    cloudWatchState.setLoadBalancerMetrics(metrics);
    Map<String, String> invalidFields = cloudWatchState.validateFields();
    assertTrue(isEmpty(cloudWatchState.fetchEcsMetrics()));
    assertTrue(isNotEmpty(cloudWatchState.fetchLoadBalancerMetrics()));
    assertTrue(isEmpty(cloudWatchState.fetchEc2Metrics()));
    assertFalse(cloudWatchState.isShouldDoLambdaVerification());
    assertFalse(cloudWatchState.isShouldDoECSClusterVerification());

    assertTrue("Size should be 0", invalidFields.size() == 0);
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateFieldsValidCaseEC2Provided() {
    CloudWatchState cloudWatchState = new CloudWatchState("dummy");
    List<CloudWatchMetric> metricList = new ArrayList<>();
    metricList.add(CloudWatchMetric.builder().metricName("asdf").build());

    cloudWatchState.setEc2Metrics(metricList);
    Map<String, String> invalidFields = cloudWatchState.validateFields();
    assertTrue(isEmpty(cloudWatchState.fetchEcsMetrics()));
    assertTrue(isEmpty(cloudWatchState.fetchLoadBalancerMetrics()));
    assertTrue(isNotEmpty(cloudWatchState.fetchEc2Metrics()));
    assertFalse(cloudWatchState.isShouldDoLambdaVerification());
    assertFalse(cloudWatchState.isShouldDoECSClusterVerification());

    assertTrue("Size should be 0", invalidFields.size() == 0);
  }
}
