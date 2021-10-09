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
