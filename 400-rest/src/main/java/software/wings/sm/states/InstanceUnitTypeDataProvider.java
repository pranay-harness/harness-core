/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static software.wings.beans.InstanceUnitType.COUNT;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;

import software.wings.stencils.DataProvider;

import com.google.common.collect.ImmutableMap;
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
