package io.harness.pms.sdk.core.adviser;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class AdvisingEvent {
  Ambiance ambiance;
  FailureInfo failureInfo;
  boolean isPreviousAdviserExpired;
  List<String> retryIds;
  byte[] adviserParameters;
  Status toStatus;
  Status fromStatus;

  public int getRetryCount() {
    if (isEmpty(retryIds)) {
      return 0;
    }
    return retryIds.size();
  }
}
