/**
 *
 */

package software.wings.app;

import com.google.common.io.Resources;
import com.google.inject.Singleton;

import org.apache.commons.codec.Charsets;
import software.wings.beans.Graph;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.net.URL;

/**
 * @author Rishi
 */
@Singleton
public class StaticConfiguration {
  /**
   * @return
   */
  public Graph defaultSimpleWorkflow() {
    URL url = this.getClass().getResource(Constants.SIMPLE_WORKFLOW_DEFAULT_GRAPH_URL);
    String json;
    try {
      json = Resources.toString(url, Charsets.UTF_8);
    } catch (IOException e) {
      throw new WingsException("Error in loading simple workflow default graph");
    }
    return JsonUtils.asObject(json, Graph.class);
  }
}
