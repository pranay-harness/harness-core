/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class AggregateFunctionLogContext extends AutoLogContext {
  public static final String ID = "aggregateFuncClass";

  public AggregateFunctionLogContext(String aggregateFuncClass, OverrideBehavior behavior) {
    super(ID, aggregateFuncClass, behavior);
  }
}
