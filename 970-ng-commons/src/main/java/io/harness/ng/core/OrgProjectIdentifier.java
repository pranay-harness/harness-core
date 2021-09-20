package io.harness.ng.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class OrgProjectIdentifier {
  private String orgIdentifier;
  private String projectIdentifier;

  public OrgProjectIdentifier(String orgProjectId) {
    String[] split = orgProjectId.split(":");
    int length = split.length;
    if (length >= 1) {
      this.orgIdentifier = split[0];
    }
    if (length >= 2) {
      this.projectIdentifier = split[1];
    }
  }

  public String getOrgIdentifier() {
    return orgIdentifier;
  }

  public String getProjectIdentifier() {
    return projectIdentifier;
  }
}
