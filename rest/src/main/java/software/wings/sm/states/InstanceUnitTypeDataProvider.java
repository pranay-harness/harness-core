package software.wings.sm.states;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.InstanceUnitType;
import software.wings.stencils.DataProvider;

import java.util.Map;

/**
 * Created by rishi on 8/13/17.
 */
public class InstanceUnitTypeDataProvider implements DataProvider {
  @Override
  public Map<String, String> getData(String appId, String... params) {
    return ImmutableMap.of(InstanceUnitType.COUNT.name(), "Count", InstanceUnitType.PERCENTAGE.name(), "Percentage");
  }
}
