/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.threading;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Schedulable implements Runnable {
  private Runnable runnable;
  private String message;

  public Schedulable(String message, Runnable runnable) {
    this.message = message;
    this.runnable = runnable;
  }

  // Some errors are actually recoverable, like not able to create thread because of lack of handles
  @SuppressWarnings({"PMD", "squid:S1181"})
  @Override
  public void run() {
    try {
      runnable.run();
    } catch (Throwable exception) {
      if (message != null) {
        log.error(message, exception);
      }
    }
  }
}
