package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class ActivityCommandUnitMetadata {
  String name;
  CommandUnitType type;
  CommandExecutionStatus status;

  @NonFinal @Setter String executionLogFile;
  @NonFinal @Getter(onMethod = @__(@JsonIgnore)) @Setter @JsonIgnore String executionLogFileContent;

  public static List<ActivityCommandUnitMetadata> fromCommandUnitDetailsList(
      List<CommandUnitDetails> commandUnitDetailsList) {
    return MetadataUtils.dedup(
        MetadataUtils.map(commandUnitDetailsList, ActivityCommandUnitMetadata::fromCommandUnitDetails),
        ActivityCommandUnitMetadata::getName);
  }

  static ActivityCommandUnitMetadata fromCommandUnitDetails(CommandUnitDetails commandUnitDetails) {
    if (commandUnitDetails == null) {
      return null;
    }

    return ActivityCommandUnitMetadata.builder()
        .name(commandUnitDetails.getName())
        .type(commandUnitDetails.getCommandUnitType())
        .status(commandUnitDetails.getCommandExecutionStatus())
        .build();
  }
}
