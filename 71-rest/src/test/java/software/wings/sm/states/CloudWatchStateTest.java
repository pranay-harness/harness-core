package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.rule.OwnerRule.PRANJAL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
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
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void testValidateFieldsInvalidCase() {
    CloudWatchState cloudWatchState = new CloudWatchState("dummy");
    // not adding any metrics for verification
    Map<String, String> invalidFields = cloudWatchState.validateFields();
    assertThat(isEmpty(cloudWatchState.fetchEcsMetrics())).isTrue();
    assertThat(isEmpty(cloudWatchState.fetchLoadBalancerMetrics())).isTrue();
    assertThat(invalidFields.size() == 1).isTrue();
    assertThat(invalidFields.keySet().iterator().next()).isEqualTo("No metrics provided");
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void testValidateFieldsValidCaseLambdaProvided() {
    CloudWatchState cloudWatchState = new CloudWatchState("dummy");
    cloudWatchState.setShouldDoLambdaVerification(true);
    Map<String, String> invalidFields = cloudWatchState.validateFields();
    assertThat(isEmpty(cloudWatchState.fetchEcsMetrics())).isTrue();
    assertThat(isEmpty(cloudWatchState.fetchLoadBalancerMetrics())).isTrue();
    assertThat(isEmpty(cloudWatchState.fetchEc2Metrics())).isTrue();
    assertThat(cloudWatchState.isShouldDoLambdaVerification()).isTrue();
    assertThat(cloudWatchState.isShouldDoECSClusterVerification()).isFalse();

    assertThat(invalidFields.size() == 0).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void testValidateFieldsValidCaseECSProvided() {
    CloudWatchState cloudWatchState = new CloudWatchState("dummy");
    cloudWatchState.setShouldDoECSClusterVerification(true);
    Map<String, String> invalidFields = cloudWatchState.validateFields();
    assertThat(isEmpty(cloudWatchState.fetchEcsMetrics())).isTrue();
    assertThat(isEmpty(cloudWatchState.fetchLoadBalancerMetrics())).isTrue();
    assertThat(isEmpty(cloudWatchState.fetchEc2Metrics())).isTrue();
    assertThat(cloudWatchState.isShouldDoECSClusterVerification()).isTrue();
    assertThat(cloudWatchState.isShouldDoLambdaVerification()).isFalse();

    assertThat(invalidFields.size() == 0).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void testValidateFieldsValidCaseLoadBalancerProvided() {
    CloudWatchState cloudWatchState = new CloudWatchState("dummy");
    Map<String, List<CloudWatchMetric>> metrics = new HashMap<>();
    List<CloudWatchMetric> metricList = new ArrayList<>();
    metricList.add(CloudWatchMetric.builder().metricName("asdf").build());
    metrics.put("loadbalancer", metricList);

    cloudWatchState.setLoadBalancerMetrics(metrics);
    Map<String, String> invalidFields = cloudWatchState.validateFields();
    assertThat(isEmpty(cloudWatchState.fetchEcsMetrics())).isTrue();
    assertThat(isNotEmpty(cloudWatchState.fetchLoadBalancerMetrics())).isTrue();
    assertThat(isEmpty(cloudWatchState.fetchEc2Metrics())).isTrue();
    assertThat(cloudWatchState.isShouldDoLambdaVerification()).isFalse();
    assertThat(cloudWatchState.isShouldDoECSClusterVerification()).isFalse();

    assertThat(invalidFields.size() == 0).isTrue();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void testValidateFieldsValidCaseEC2Provided() {
    CloudWatchState cloudWatchState = new CloudWatchState("dummy");
    List<CloudWatchMetric> metricList = new ArrayList<>();
    metricList.add(CloudWatchMetric.builder().metricName("asdf").build());

    cloudWatchState.setEc2Metrics(metricList);
    Map<String, String> invalidFields = cloudWatchState.validateFields();
    assertThat(isEmpty(cloudWatchState.fetchEcsMetrics())).isTrue();
    assertThat(isEmpty(cloudWatchState.fetchLoadBalancerMetrics())).isTrue();
    assertThat(isNotEmpty(cloudWatchState.fetchEc2Metrics())).isTrue();
    assertThat(cloudWatchState.isShouldDoLambdaVerification()).isFalse();
    assertThat(cloudWatchState.isShouldDoECSClusterVerification()).isFalse();

    assertThat(invalidFields.size() == 0).isTrue();
  }
}
