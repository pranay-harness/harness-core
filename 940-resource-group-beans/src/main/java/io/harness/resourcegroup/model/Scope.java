package io.harness.resourcegroup.model;

import static org.apache.commons.lang3.StringUtils.stripToNull;

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
    if (Objects.nonNull(stripToNull(accountIdentifier))) {
      scope = Scope.ACCOUNT;
    }
    if (scope != null && Objects.nonNull(stripToNull(orgIdentifier))) {
      scope = Scope.ORGANIZATION;
    }
    if (scope != null && Objects.nonNull(stripToNull(projectIdentifier))) {
      scope = Scope.PROJECT;
    }
    return scope;
  }
}
