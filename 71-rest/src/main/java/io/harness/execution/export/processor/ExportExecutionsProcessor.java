package io.harness.execution.export.processor;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.export.metadata.ExecutionMetadata;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface ExportExecutionsProcessor {
  void visitExecutionMetadata(@NotNull ExecutionMetadata executionMetadata);
  void process();
}
