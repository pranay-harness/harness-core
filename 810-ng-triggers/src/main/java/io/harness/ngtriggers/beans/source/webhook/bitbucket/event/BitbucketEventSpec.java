package io.harness.ngtriggers.beans.source.webhook.bitbucket.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = BitbucketPRSpec.class, name = "Pull Request")
  , @JsonSubTypes.Type(value = BitbucketPushSpec.class, name = "Push")
})
@OwnedBy(PIPELINE)
public interface BitbucketEventSpec {}
