package software.wings.stencils;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 6/27/16.
 */
public interface EnumDataProvider {
  default Map
    <String, String> getDataForEnum(String appId) {
      return getDataForEnum(appId, null);
    }

    Map<String, String> getDataForEnum(String appId, String... params);
}
