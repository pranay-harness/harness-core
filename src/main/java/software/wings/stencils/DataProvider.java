package software.wings.stencils;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 6/27/16.
 */
public interface DataProvider {
  default Map
    <String, String> getData(String appId) {
      return getData(appId, null);
    }

    Map<String, String> getData(String appId, String... params);
}
