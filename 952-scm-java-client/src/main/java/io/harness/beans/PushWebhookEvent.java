/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.beans.WebhookEvent.Type.PUSH;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("Push")
@OwnedBy(DX)
public class PushWebhookEvent implements WebhookEvent {
  private String branchName;
  private String link;
  private List<CommitDetails> commitDetailsList;
  private Repository repository;
  private WebhookBaseAttributes baseAttributes;

  @Override
  public Type getType() {
    return PUSH;
  }
}
