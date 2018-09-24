/**
 *
 */

package software.wings.app;

import com.google.common.io.Resources;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import software.wings.beans.Graph;
import software.wings.common.Constants;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * The type Static configuration.
 *
 * @author Rishi
 */
@Singleton
public class StaticConfiguration {
  /**
   * Default simple workflow graph.
   *
   * @return graph graph
   */
  public Graph defaultSimpleWorkflow() {
    URL url = this.getClass().getResource(Constants.SIMPLE_WORKFLOW_DEFAULT_GRAPH_URL);
    String json;
    try {
      json = Resources.toString(url, Charset.defaultCharset());
    } catch (IOException e) {
      throw new WingsException("Error in loading simple workflow default graph", e);
    }
    return JsonUtils.asObject(json, Graph.class);
  }
}
