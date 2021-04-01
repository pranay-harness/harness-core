package io.harness.cdng.pipeline.executions.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.pms.contracts.cd.CDPipelineModuleInfoProto;
import io.harness.pms.contracts.cd.EnvironmentTypeProto;
import io.harness.pms.contracts.service.PipelineModuleInfoProto;
import io.harness.pms.sdk.execution.beans.PipelineModuleInfo;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
public class CDPipelineModuleInfo implements PipelineModuleInfo {
  @Singular private List<String> serviceIdentifiers;
  @Singular private List<String> envIdentifiers;
  @Singular private List<String> serviceDefinitionTypes;
  @Singular private List<EnvironmentType> environmentTypes;
  @Singular private List<String> infrastructureTypes;

  @Override
  public PipelineModuleInfoProto toProto() {
    CDPipelineModuleInfoProto.Builder builder = CDPipelineModuleInfoProto.newBuilder();
    if (EmptyPredicate.isNotEmpty(serviceIdentifiers)) {
      builder.addAllServiceIdentifiers(
          serviceIdentifiers.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }
    if (EmptyPredicate.isNotEmpty(envIdentifiers)) {
      builder.addAllEnvIdentifiers(envIdentifiers.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }
    if (EmptyPredicate.isNotEmpty(serviceDefinitionTypes)) {
      builder.addAllServiceDefinitionTypes(
          serviceDefinitionTypes.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }
    if (EmptyPredicate.isNotEmpty(environmentTypes)) {
      builder.addAllEnvironmentTypes(environmentTypes.stream()
                                         .filter(Objects::nonNull)
                                         .map(CDPipelineModuleInfo::envTypeToProto)
                                         .collect(Collectors.toList()));
    }
    if (EmptyPredicate.isNotEmpty(infrastructureTypes)) {
      builder.addAllInfrastructureTypes(
          infrastructureTypes.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }
    return PipelineModuleInfoProto.newBuilder().setCdPipelineModuleInfo(builder.build()).build();
  }

  private static EnvironmentTypeProto envTypeToProto(EnvironmentType envType) {
    switch (envType) {
      case PreProduction:
        return EnvironmentTypeProto.PreProduction;
      case Production:
        return EnvironmentTypeProto.Production;
      default:
        throw new UnsupportedOperationException("Unsupported env type");
    }
  }
}
