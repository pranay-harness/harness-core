package io.harness.ngtriggers.beans.source.webhook.v2.gitlab.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAware;
import io.harness.ngtriggers.beans.source.webhook.v2.git.PayloadAware;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GitlabPRSpec.class, name = "Merge Request")
  , @JsonSubTypes.Type(value = GitlabPushSpec.class, name = "Push")
})
@OwnedBy(PIPELINE)
public interface GitlabEventSpec extends PayloadAware, GitAware {}
