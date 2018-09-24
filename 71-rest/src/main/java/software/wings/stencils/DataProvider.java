package software.wings.stencils;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 6/27/16.
 */
public interface DataProvider {
  /**
   * Gets data.
   *
   * @param appId  the app id
   * @param params the params
   * @return the data
   */
  Map<String, String> getData(String appId, Map<String, String> params);
}
