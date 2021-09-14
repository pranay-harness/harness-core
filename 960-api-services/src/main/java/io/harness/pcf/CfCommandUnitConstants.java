/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface CfCommandUnitConstants {
  String FetchFiles = "Download Manifest Files";
  String FetchGitFiles = "Download Git Manifest Files";
  String FetchCustomFiles = "Download Custom Manifest Files";
  String CheckExistingApps = "Check Existing Applications";
  String PcfSetup = "Setup Application";
  String Wrapup = "Wrap up";
  String Pcfplugin = "Execute CF Command";
  String Downsize = "Downsize Application";
  String Upsize = "Upsize Application";
}
