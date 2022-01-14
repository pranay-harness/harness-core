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
