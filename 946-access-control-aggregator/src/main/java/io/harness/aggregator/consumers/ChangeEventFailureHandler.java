/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.aggregator.consumers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.debezium.engine.ChangeEvent;

@OwnedBy(HarnessTeam.PL)
public interface ChangeEventFailureHandler {
  void handle(ChangeEvent<String, String> changeEvent, Throwable exception);
}
