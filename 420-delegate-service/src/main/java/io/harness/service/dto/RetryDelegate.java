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
