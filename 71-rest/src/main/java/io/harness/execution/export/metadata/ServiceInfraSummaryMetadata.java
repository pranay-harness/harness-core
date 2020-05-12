package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;

import com.google.common.annotations.VisibleForTesting;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.api.ServiceElement;
import software.wings.beans.ElementExecutionSummary;
import software.wings.sm.ContextElement;
import software.wings.sm.InstanceStatusSummary;

import java.util.List;
import java.util.Objects;

@OwnedBy(CDC)
@Value
@Builder
public class ServiceInfraSummaryMetadata {
  String service;
  List<InfraMetadata> infrastructure;
  Long instancesDeployed;

  static List<ServiceInfraSummaryMetadata> fromElementExecutionSummaries(
      List<ElementExecutionSummary> executionSummaries, boolean infraRefactor) {
    return MetadataUtils.map(
        executionSummaries, executionSummary -> fromElementExecutionSummary(executionSummary, infraRefactor));
  }

  @VisibleForTesting
  static ServiceInfraSummaryMetadata fromElementExecutionSummary(
      ElementExecutionSummary executionSummary, boolean infraRefactor) {
    if (executionSummary == null) {
      return null;
    }

    ContextElement contextElement = executionSummary.getContextElement();
    if (!(contextElement instanceof ServiceElement)) {
      return null;
    }

    ServiceElement serviceElement = (ServiceElement) contextElement;
    long instancesDeployed = emptyIfNull(executionSummary.getInstanceStatusSummaries())
                                 .stream()
                                 .filter(Objects::nonNull)
                                 .map(InstanceStatusSummary::getInstanceElement)
                                 .filter(Objects::nonNull)
                                 .count();
    return ServiceInfraSummaryMetadata.builder()
        .service(serviceElement.getName())
        .infrastructure(InfraMetadata.fromElementExecutionSummary(executionSummary, infraRefactor))
        .instancesDeployed(instancesDeployed <= 0 ? 0 : instancesDeployed)
        .build();
  }
}
