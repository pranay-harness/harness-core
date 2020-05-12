package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
@OwnedBy(CDC)
public interface ExecutionDetailsMetadata {
  String getName();
  String getActivityId();
  List<ActivityCommandUnitMetadata> getSubCommands();
  void setSubCommands(List<ActivityCommandUnitMetadata> subCommands);
}
