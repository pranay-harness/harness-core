package io.harness.cdng.pipeline.executions.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
@RecasterAlias("io.harness.cdng.pipeline.executions.beans.CDPipelineModuleInfo")
@OwnedBy(HarnessTeam.CDP)
public class CDPipelineModuleInfo implements PipelineModuleInfo {
  @Singular private List<String> serviceIdentifiers;
  @Singular private List<String> envIdentifiers;
  @Singular private List<String> serviceDefinitionTypes;
  @Singular private List<EnvironmentType> environmentTypes;
  @Singular private List<String> infrastructureTypes;
}
