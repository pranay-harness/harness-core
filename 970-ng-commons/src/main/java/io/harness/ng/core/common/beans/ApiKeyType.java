package io.harness.ng.core.common.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@OwnedBy(PL)
public enum ApiKeyType {
  USER("pat"),
  SERVICE_ACCOUNT("sat");

  @Getter public final String value;

  ApiKeyType(String value) {
    this.value = value;
  }
}
