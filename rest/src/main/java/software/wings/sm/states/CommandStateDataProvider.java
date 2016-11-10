package software.wings.sm.states;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import software.wings.stencils.DataProvider;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 6/30/16.
 */
@Singleton
public class CommandStateDataProvider implements DataProvider {
  @Override
  public Map<String, String> getData(String appId, String... params) {
    return ImmutableMap.of("Start", "Start", "Stop", "Stop", "Install", "Install", "", "Command");
  }
}
