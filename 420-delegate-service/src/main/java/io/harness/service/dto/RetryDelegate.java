/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.dto;

import io.harness.beans.DelegateTask;

import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.query.Query;

@Value
@Builder
public class RetryDelegate {
  String delegateId;
  Query<DelegateTask> taskQuery;
  DelegateTask delegateTask;
  boolean retryPossible;
}
