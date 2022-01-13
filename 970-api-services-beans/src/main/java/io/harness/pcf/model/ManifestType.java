/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
