/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delay;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.tasks.ResponseData;
import io.harness.waiter.WaitNotifyEngine;

import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class SimpleNotifier implements Runnable {
  private WaitNotifyEngine waitNotifyEngine;
  private String correlationId;
  private ResponseData response;

  /**
   * Instantiates a new Simple notifier.
   *
   * @param waitNotifyEngine the wait notify engine
   * @param correlationId    the correlation id
   * @param response         the response
   */
  public SimpleNotifier(WaitNotifyEngine waitNotifyEngine, String correlationId, ResponseData response) {
    this.waitNotifyEngine = waitNotifyEngine;
    this.correlationId = correlationId;
    this.response = response;
  }

  @Override
  public void run() {
    log.info("Simple Notifier Notifying on correlation id : {}", correlationId);
    waitNotifyEngine.doneWith(correlationId, response);
  }
}
