package io.harness.ng.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
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

  public OrgProjectIdentifier(String orgIdentifier, String projectIdentifier) {
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
  }

  public String getOrgIdentifier() {
    return orgIdentifier;
  }

  public String getProjectIdentifier() {
    return projectIdentifier;
  }

  @Override
  public String toString() {
    return this.orgIdentifier + ":" + this.projectIdentifier;
  }
}
