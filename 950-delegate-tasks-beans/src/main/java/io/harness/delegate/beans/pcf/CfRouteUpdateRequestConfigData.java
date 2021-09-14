/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class CfRouteUpdateRequestConfigData {
  private String newApplicationName;
  private List<CfAppSetupTimeDetails> existingApplicationDetails;
  private List<String> existingApplicationNames;
  private List<String> tempRoutes;
  private List<String> finalRoutes;
  private boolean isRollback;
  private boolean isStandardBlueGreen;
  private boolean downsizeOldApplication;
  private boolean isMapRoutesOperation;
  private boolean skipRollback;
  private boolean upSizeInActiveApp;
  private boolean versioningChanged;
  private boolean nonVersioning;
  private CfAppSetupTimeDetails existingInActiveApplicationDetails;
  private CfAppSetupTimeDetails newApplicationDetails;
  private String cfAppNamePrefix;
  private String existingAppNamingStrategy;
}
