/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.metrics.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.metrics.AutoMetricContext;
import io.harness.metrics.beans.DelegateTaskMetricContext;
import io.harness.metrics.beans.DelegateTaskResponseMetricContext;
import io.harness.metrics.intfc.DelegateMetricsService;
import io.harness.metrics.service.api.MetricService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateMetricsServiceImpl implements DelegateMetricsService {
  public static final String DELEGATE_TASK_CREATION = "delegate_task_creation";
  public static final String DELEGATE_TASK_NO_ELIGIBLE_DELEGATES = "delegate_task_no_eligible_delegates";
  public static final String DELEGATE_TASK_RESPONSE = "delegate_response";
  public static final String DELEGATE_TASK_ACQUIRE = "delegate_task_acquire";
  public static final String DELEGATE_TASK_ACQUIRE_FAILED = "delegate_task_acquire_failed";
  public static final String DELEGATE_TASK_EXPIRED = "delegate_task_expired";
  public static final String DELEGATE_TASK_ACQUIRE_LIMIT_EXCEEDED = "delegate_task_acquire_limit_exceeded";

  public static final String DELEGATE_REGISTRATION = "delegate_registration";
  public static final String DELEGATE_REGISTRATION_FAILED = "delegate_registration_failed";

  @Inject private MetricService metricService;
  @Inject private DelegateTaskMetricContextBuilder metricContextBuilder;

  @Override
  public void recordDelegateTaskMetrics(DelegateTask task, String metricName) {
    try (AutoMetricContext ignore = metricContextBuilder.getContext(task, DelegateTask.class)) {
      metricService.incCounter(metricName);
    }
  }

  @Override
  public void recordDelegateTaskMetrics(String accountId, String metricName) {
    try (DelegateTaskMetricContext ignore = new DelegateTaskMetricContext(accountId)) {
      metricService.incCounter(metricName);
    }
  }

  @Override
  public void recordDelegateTaskResponseMetrics(
      DelegateTask delegateTask, DelegateTaskResponse response, String metricName) {
    try (DelegateTaskResponseMetricContext ignore = new DelegateTaskResponseMetricContext(delegateTask, response)) {
      metricService.incCounter(metricName);
    }
  }

  @Override
  public void recordDelegateMetrics(Delegate delegate, String metricName) {
    try (AutoMetricContext ignore = metricContextBuilder.getContext(delegate, Delegate.class)) {
      metricService.incCounter(metricName);
    }
  }
}
