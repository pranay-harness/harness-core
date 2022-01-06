/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.pcf.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.task.pcf.CfCommandResponse;
import io.harness.logging.CommandExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This class represents response from PcfCommandTask.SETP
 * It returns guid for new application created, name and
 */
@Data
@EqualsAndHashCode(callSuper = false)
@OwnedBy(CDP)
public class CfSetupCommandResponse extends CfCommandResponse {
  private CfAppSetupTimeDetails newApplicationDetails;
  private Integer totalPreviousInstanceCount;
  private List<CfAppSetupTimeDetails> downsizeDetails;
  private Integer instanceCountForMostRecentVersion;
  private CfAppSetupTimeDetails mostRecentInactiveAppVersion;
  private boolean versioningChanged;
  private boolean nonVersioning;
  private Integer activeAppRevision;
  private String existingAppNamingStrategy;

  @Builder
  public CfSetupCommandResponse(CommandExecutionStatus commandExecutionStatus, String output,
      CfAppSetupTimeDetails newApplicationDetails, Integer totalPreviousInstanceCount,
      List<CfAppSetupTimeDetails> downsizeDetails, Integer instanceCountForMostRecentVersion,
      CfAppSetupTimeDetails mostRecentInactiveAppVersion, boolean versioningChanged, boolean nonVersioning,
      Integer activeAppRevision, String existingAppNamingStrategy) {
    super(commandExecutionStatus, output);
    this.newApplicationDetails = newApplicationDetails;
    this.totalPreviousInstanceCount = totalPreviousInstanceCount;
    this.downsizeDetails = downsizeDetails;
    this.instanceCountForMostRecentVersion = instanceCountForMostRecentVersion;
    this.mostRecentInactiveAppVersion = mostRecentInactiveAppVersion;
    this.versioningChanged = versioningChanged;
    this.nonVersioning = nonVersioning;
    this.activeAppRevision = activeAppRevision;
    this.existingAppNamingStrategy = existingAppNamingStrategy;
  }
}
