package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public enum ScopeLevel {
  ACCOUNT("ACCOUNT"),
  ORGANIZATION("ORGANIZATION"),
  PROJECT("PROJECT");

  String name;

  ScopeLevel(String name) {
    this.name = name;
  }

  public static ScopeLevel of(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (!isBlank(projectIdentifier)) {
      return ScopeLevel.PROJECT;
    }
    if (!isBlank(orgIdentifier)) {
      return ScopeLevel.ORGANIZATION;
    }
    return ScopeLevel.ACCOUNT;
  }
}
