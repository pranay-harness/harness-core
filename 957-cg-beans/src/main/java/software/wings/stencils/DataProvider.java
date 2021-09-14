/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.stencils;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 6/27/16.
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
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
