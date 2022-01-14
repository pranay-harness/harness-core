package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface ExecutionMetadata extends GraphNodeVisitable {
  String getId();
  String getExecutionType();
  String getAppId();
  String getApplication();
  String getEntityName();
  TimingMetadata getTiming();
}
