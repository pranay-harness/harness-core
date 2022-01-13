/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;

import io.harness.annotations.dev.OwnedBy;

import software.wings.api.ServiceElement;
import software.wings.beans.ElementExecutionSummary;
import software.wings.sm.ContextElement;
import software.wings.sm.InstanceStatusSummary;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class ServiceInfraSummaryMetadata {
  String service;
  List<InfraMetadata> infrastructure;
  Long instancesDeployed;

  static List<ServiceInfraSummaryMetadata> fromElementExecutionSummaries(
      List<ElementExecutionSummary> executionSummaries) {
    return MetadataUtils.map(executionSummaries, executionSummary -> fromElementExecutionSummary(executionSummary));
  }

  @VisibleForTesting
  static ServiceInfraSummaryMetadata fromElementExecutionSummary(ElementExecutionSummary executionSummary) {
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
        .infrastructure(InfraMetadata.fromElementExecutionSummary(executionSummary))
        .instancesDeployed(instancesDeployed <= 0 ? 0 : instancesDeployed)
        .build();
  }
}
