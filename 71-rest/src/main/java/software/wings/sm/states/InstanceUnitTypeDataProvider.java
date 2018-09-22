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
  public Map<String, String> getData(String appId, Map<String, String> params) {
    return ImmutableMap.of(COUNT.name(), "Count", PERCENTAGE.name(), "Percent");
  }
}
