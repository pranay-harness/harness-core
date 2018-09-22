package software.wings.appdynamics;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetric.AppdynamicsMetricType;
import software.wings.utils.JsonUtils;

import java.util.List;

/**
 * Created by rsingh on 4/19/17.
 */
public class AppdynamicsMetricTest {
  @Test
  public void testAppdynamicsMetricParse() throws Exception {
    final String metricResponse = "[{\"name\": \"Average Response Time (ms)\", \"type\": \"leaf\"},"
        + "{\"name\": \"Calls per Minute\",\"type\": \"folder\"},"
        + "{\"name\": \"Errors per Minute\",\"type\": \"leaf\"},"
        + "{\"name\": \"Number of Slow Calls\",\"type\": \"folder\"}]";

    final List<AppdynamicsMetric> metrics =
        JsonUtils.asObject(metricResponse, new TypeReference<List<AppdynamicsMetric>>() {});
    assertEquals(4, metrics.size());

    assertEquals(AppdynamicsMetricType.leaf, metrics.get(0).getType());
    assertEquals("Average Response Time (ms)", metrics.get(0).getName());

    assertEquals(AppdynamicsMetricType.folder, metrics.get(1).getType());
    assertEquals("Calls per Minute", metrics.get(1).getName());

    assertEquals(AppdynamicsMetricType.leaf, metrics.get(2).getType());
    assertEquals("Errors per Minute", metrics.get(2).getName());

    assertEquals(AppdynamicsMetricType.folder, metrics.get(3).getType());
    assertEquals("Number of Slow Calls", metrics.get(3).getName());
  }
}
