package io.harness.resourcegroup.model;

import java.util.Objects;

public enum Scope {
  ACCOUNT("ACCOUNT"),
  ORGANIZATION("ORGANIZATION"),
  PROJECT("PROJECT");

  String name;

  Scope(String name) {
    this.name = name;
  }

  public static Scope ofResourceGroup(ResourceGroup resourceGroup) {
    return of(
        resourceGroup.getAccountIdentifier(), resourceGroup.getOrgIdentifier(), resourceGroup.getProjectIdentifier());
  }

  public static Scope of(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Scope scope = null;
    if (Objects.nonNull(accountIdentifier)) {
      scope = Scope.ACCOUNT;
    }
    if (Objects.nonNull(orgIdentifier)) {
      scope = Scope.ORGANIZATION;
    }
    if (Objects.nonNull(projectIdentifier)) {
      scope = Scope.PROJECT;
    }
    return scope;
  }
}
