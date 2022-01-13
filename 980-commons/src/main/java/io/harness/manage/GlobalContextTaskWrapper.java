/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.manage;

import io.harness.context.GlobalContext;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GlobalContextTaskWrapper implements Runnable {
  private Runnable task;
  private GlobalContext context;

  @Override
  public void run() {
    try (GlobalContextGuard guard = new GlobalContextGuard(context)) {
      task.run();
    }
  }
}
