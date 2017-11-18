package software.wings.service.impl;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import software.wings.stencils.DataProvider;

import java.util.Map;

/**
 * Created by sgurubelli on 11/16/17.
 */
@Singleton
public class NexusVersionProvider implements DataProvider {
  @Override
  public Map<String, String> getData(String appId, String... params) {
    return ImmutableMap.of("2.X", "2.X", "3.X", "3.X");
  }
}
