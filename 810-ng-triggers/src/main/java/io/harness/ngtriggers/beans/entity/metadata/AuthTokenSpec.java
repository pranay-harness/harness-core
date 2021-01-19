package io.harness.ngtriggers.beans.entity.metadata;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = CustomWebhookInlineAuthToken.class, name = "inline")
  , @JsonSubTypes.Type(value = CustomWebhookReferencedTokenSpec.class, name = "ref")
})
@JsonDeserialize()
public interface AuthTokenSpec {}
