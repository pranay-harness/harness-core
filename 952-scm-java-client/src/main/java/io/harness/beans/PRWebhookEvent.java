/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.beans.WebhookEvent.Type.PR;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("PR")
@OwnedBy(DX)
public class PRWebhookEvent implements WebhookEvent {
  private Long pullRequestId;
  private String pullRequestLink;
  private String pullRequestBody;
  private String sourceBranch;
  private String targetBranch;
  private String title;
  private boolean closed;
  private boolean merged;
  private List<CommitDetails> commitDetailsList;
  private Repository repository;
  private WebhookBaseAttributes baseAttributes;

  @Override
  public Type getType() {
    return PR;
  }
}
