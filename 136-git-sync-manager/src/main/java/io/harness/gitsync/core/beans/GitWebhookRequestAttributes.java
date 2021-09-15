/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.core.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"webhookBody", "webhookHeaders"})
@EqualsAndHashCode(callSuper = false)
@Entity(value = "gitWebhookRequestAttributesNG", noClassnameStored = true)
@Document("gitWebhookRequestAttributesNG")
@TypeAlias("io.harness.gitsync.core.beans.gitWebhookRequestAttributes")
@OwnedBy(DX)
public class GitWebhookRequestAttributes {
  private String webhookBody;
  private String webhookHeaders;
  @NotEmpty private String branchName;
  private String gitConnectorId;
  @NotEmpty private String repo;
  String headCommitId;
}
