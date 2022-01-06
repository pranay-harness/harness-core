/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

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
