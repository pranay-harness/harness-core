/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.waiter;

import io.harness.tasks.ResponseData;

import java.util.Map;

/**
 * Function to call when all correlationIds are completed for a wait instance.
 */
public interface OldNotifyCallback extends NotifyCallback {
  void notify(Map<String, ResponseData> response);
  void notifyError(Map<String, ResponseData> response);
}
