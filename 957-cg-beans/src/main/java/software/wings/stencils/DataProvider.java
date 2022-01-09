/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
