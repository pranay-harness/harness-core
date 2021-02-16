package io.harness.resourcegroup.resourceclient.api;

import io.harness.resourcegroup.model.Scope;

import java.util.List;
import java.util.Set;

public interface ResourceValidator {
  String getResourceType();

  Set<Scope> getScopes();

  List<Boolean> validate(
      List<String> resourceIds, String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
