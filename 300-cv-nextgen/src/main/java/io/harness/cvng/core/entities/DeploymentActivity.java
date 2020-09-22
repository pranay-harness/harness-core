package io.harness.cvng.core.entities;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.base.Preconditions;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.Set;

@JsonTypeName("DEPLOYMENT")
@Data
@FieldNameConstants(innerTypeName = "DeploymentActivityKeys")
@Builder
@AllArgsConstructor
public class DeploymentActivity extends Activity {
  Long dataCollectionDelayMs;
  Set<String> oldVersionHosts;
  Set<String> newVersionHosts;
  Integer newHostsTrafficSplitPercentage;

  @Override
  public ActivityType getType() {
    return ActivityType.DEPLOYMENT;
  }

  @Override
  public void validateActivityParams() {
    Preconditions.checkNotNull(getVerificationJobRuntimeDetails(),
        "Verification job details cannot be "
            + "empty for a deployment activity");
    getVerificationJobRuntimeDetails().forEach(verificationJob -> {
      Preconditions.checkNotNull(isEmpty(verificationJob.getVerificationJobIdentifier()),
          "The verification job identifier is a required parameter for deployment activities");
    });
  }
}
