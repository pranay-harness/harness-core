/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.datahandler.models;

import static io.harness.annotations.dev.HarnessModule._955_ACCOUNT_MGMT;
import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.license.CeLicenseInfo;
import io.harness.ng.core.account.DefaultExperience;

import software.wings.beans.LicenseInfo;

import lombok.Data;

@OwnedBy(GTM)
@Data
@TargetModule(_955_ACCOUNT_MGMT)
public class AccountDetails {
  private String accountId;
  private String accountName;
  private String companyName;
  private String cluster;
  private boolean createdFromNG;
  private DefaultExperience defaultExperience;
  private LicenseInfo licenseInfo;
  private CeLicenseInfo ceLicenseInfo;
  private int activeServiceCount;
}
