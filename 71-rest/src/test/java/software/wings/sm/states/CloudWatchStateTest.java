package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

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
  public void testValidateFieldsValidCase() {
    CloudWatchState cloudWatchState = new CloudWatchState("dummy");
    cloudWatchState.setShouldDoLambdaVerification(true);
    Map<String, String> invalidFields = cloudWatchState.validateFields();
    assertTrue(isEmpty(cloudWatchState.fetchEcsMetrics()));
    assertTrue(isEmpty(cloudWatchState.fetchLoadBalancerMetrics()));
    assertTrue(cloudWatchState.isShouldDoLambdaVerification());
    assertTrue("Size should be 0", invalidFields.size() == 0);
  }
}
