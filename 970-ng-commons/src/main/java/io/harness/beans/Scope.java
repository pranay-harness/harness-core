package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public enum Scope {
  ACCOUNT("ACCOUNT"),
  ORGANIZATION("ORGANIZATION"),
  PROJECT("PROJECT");

  String name;

  Scope(String name) {
    this.name = name;
  }

  public static Scope of(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (!isBlank(projectIdentifier)) {
      return Scope.PROJECT;
    }
    if (!isBlank(orgIdentifier)) {
      return Scope.ORGANIZATION;
    }
    return Scope.ACCOUNT;
  }
}
