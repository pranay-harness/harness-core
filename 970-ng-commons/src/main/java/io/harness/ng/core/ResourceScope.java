package io.harness.ng.core;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;

@Getter
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "scope", visible = true)
public abstract class ResourceScope {
  String scope;

  public ResourceScope(String scope) {
    this.scope = scope;
  }
}
