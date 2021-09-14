/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.app.schema.type.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(DEL)
public enum QLDelegateType implements QLEnum {
  SHELL_SCRIPT,
  DOCKER,
  KUBERNETES,
  HELM_DELEGATE,
  ECS;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
