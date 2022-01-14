package io.harness.aggregator.consumers;

import lombok.Value;

@Value
class Result {
  long numberOfACLsCreated;
  long numberOfACLsDeleted;
}
