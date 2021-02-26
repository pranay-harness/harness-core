package io.harness.accesscontrol.clients;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", include = PROPERTY)
@JsonSubTypes({ @JsonSubTypes.Type(value = HAccessControlDTO.class) })
public interface AccessControlDTO {}
