package io.harness.cdng.instance.outcome;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Value
@Builder
@TypeAlias("deploymentInfoOutcome")
@JsonTypeName("deploymentInfoOutcome")
public class DeploymentInfoOutcome implements Outcome, ExecutionSweepingOutput {
  List<ServerInstanceInfo> serverInstanceInfoList;
}
