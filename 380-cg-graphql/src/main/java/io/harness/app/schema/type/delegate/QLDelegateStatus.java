package io.harness.app.schema.type.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(DEL)
public enum QLDelegateStatus implements QLEnum {
  ENABLED,
  WAITING_FOR_APPROVAL,
  DELETED;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
