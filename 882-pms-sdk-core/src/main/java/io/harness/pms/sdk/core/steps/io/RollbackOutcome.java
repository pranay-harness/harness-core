package io.harness.pms.sdk.core.steps.io;

import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("rollbackOutcome")
@JsonTypeName("rollbackOutcome")
public class RollbackOutcome implements Outcome {
  RollbackInfo rollbackInfo;

  @Override
  public String getType() {
    return "rollbackOutcome";
  }

  public static RollbackOutcome getClonedRollbackOutcome(RollbackInfo rollbackInfo, String identifier, String group) {
    return RollbackOutcome.builder().rollbackInfo(rollbackInfo.getRollbackInfo(identifier, group)).build();
  }
  @Override
  public String toViewJson() {
    return null;
  }
}
