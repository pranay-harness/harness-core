/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.metrics.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.NgSetupFields.NG;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.metrics.AutoMetricContext;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class DelegateTaskResponseMetricContext extends AutoMetricContext {
  public DelegateTaskResponseMetricContext(DelegateTask delegateTask, DelegateTaskResponse response) {
    boolean ng = !isEmpty(delegateTask.getSetupAbstractions())
        && Boolean.parseBoolean(delegateTask.getSetupAbstractions().get(NG));
    put("accountId", response.getAccountId());
    put("responseCode", response.getResponseCode().name());
    put("taskType", delegateTask.getData().getTaskType());
    put("async", String.valueOf(delegateTask.getData().isAsync()));
    put("ng", String.valueOf(ng));
  }
}
