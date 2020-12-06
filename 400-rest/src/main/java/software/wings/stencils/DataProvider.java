package software.wings.stencils;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 6/27/16.
 */
@OwnedBy(CDC)
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
