package io.harness.pcf.model;

import lombok.Getter;

@Getter
public enum ManifestType {
  APPLICATION_MANIFEST("Application manifest"),
  APPLICATION_MANIFEST_WITH_CREATE_SERVICE("Application manifest with create service"),
  CREATE_SERVICE_MANIFEST("Create service manifest"),
  VARIABLE_MANIFEST("Variable"),
  AUTOSCALAR_MANIFEST("App autoscaler manifest");

  private final String description;
  ManifestType(String description) {
    this.description = description;
  }
}
