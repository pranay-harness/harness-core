/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.pcf.response.CfDeployCommandResponse;
import io.harness.delegate.task.pcf.response.CfInfraMappingDataResponse;
import io.harness.delegate.task.pcf.response.CfInstanceSyncResponse;
import io.harness.delegate.task.pcf.response.CfSetupCommandResponse;
import io.harness.logging.CommandExecutionStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = CfDeployCommandResponse.class, name = "cfDeployCommandResponse")
  , @JsonSubTypes.Type(value = CfInfraMappingDataResponse.class, name = "cfInfraMappingDataResponse"),
      @JsonSubTypes.Type(value = CfInstanceSyncResponse.class, name = "cfInstanceSyncResponse"),
      @JsonSubTypes.Type(value = CfSetupCommandResponse.class, name = "cfSetupCommandResponse")
})
@OwnedBy(CDP)
public class CfCommandResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
