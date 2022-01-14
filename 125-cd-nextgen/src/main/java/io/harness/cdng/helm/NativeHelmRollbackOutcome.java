package io.harness.cdng.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.container.ContainerInfo;
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
@TypeAlias("NativeHelmRollbackOutcome")
@JsonTypeName("NativeHelmRollbackOutcome")
@RecasterAlias("io.harness.cdng.helm.NativeHelmRollbackOutcome")
public class NativeHelmRollbackOutcome implements Outcome, ExecutionSweepingOutput {
  String releaseName;
  int newReleaseVersion;
  int rollbackVersion;
  List<ContainerInfo> containerInfoList;
}
