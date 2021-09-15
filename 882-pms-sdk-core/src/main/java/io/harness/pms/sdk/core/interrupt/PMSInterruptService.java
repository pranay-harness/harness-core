/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.interrupt;

public interface PMSInterruptService {
  void handleAbort(String notifyId);
  void handleFailure(String notifyId);
}
