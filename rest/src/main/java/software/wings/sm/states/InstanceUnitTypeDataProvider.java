package software.wings.sm.states;

import static software.wings.beans.InstanceUnitType.COUNT;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;

import com.google.common.collect.ImmutableMap;

import software.wings.stencils.DataProvider;

import java.util.Map;

/**
 * Created by rishi on 8/13/17.
 */
public class InstanceUnitTypeDataProvider implements DataProvider {
  @Override
  public Map<String, String> getData(String appId, String... params) {
    // TODO: make it plain english
    return ImmutableMap.of(COUNT.name(), COUNT.name(), PERCENTAGE.name(), PERCENTAGE.name());
  }
}
