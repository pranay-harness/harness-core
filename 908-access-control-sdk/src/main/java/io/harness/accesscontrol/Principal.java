package io.harness.accesscontrol;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import io.harness.accesscontrol.principals.PrincipalType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(name = "HPrincipal", value = HPrincipal.class) })
public interface Principal {
  PrincipalType getPrincipalType();
  String getPrincipalIdentifier();
}
