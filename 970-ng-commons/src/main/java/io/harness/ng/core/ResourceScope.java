package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@OwnedBy(PL)
@Getter
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "scope", visible = true)
@EqualsAndHashCode(callSuper = false)
public abstract class ResourceScope {
  String scope;

  public ResourceScope(String scope) {
    this.scope = scope;
  }
}
