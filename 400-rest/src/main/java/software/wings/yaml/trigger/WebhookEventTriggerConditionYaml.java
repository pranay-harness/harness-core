/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("WEBHOOK")
@JsonPropertyOrder({"harnessApiVersion"})
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class WebhookEventTriggerConditionYaml extends TriggerConditionYaml {
  private String repositoryType;
  private List<String> eventType = new ArrayList<>();
  private List<String> action = new ArrayList<>();
  private List<String> releaseActions = new ArrayList<>();
  private String branchRegex;
  private String gitConnectorId;
  private String gitConnectorName;
  private String repoName;
  private String branchName;
  private List<String> filePaths;
  private Boolean checkFileContentChanged;
  private String webhookSecret;

  public WebhookEventTriggerConditionYaml() {
    super.setType("WEBHOOK");
  }

  @lombok.Builder
  WebhookEventTriggerConditionYaml(String repositoryType, String branchRegex, List<String> eventType,
      List<String> action, List<String> releaseActions, String gitConnectorId, String gitConnectorName, String repoName,
      String branchName, List<String> filePaths, Boolean checkFileContentChanged, String webhookSecret) {
    super.setType("WEBHOOK");
    this.eventType = eventType;
    this.action = action;
    this.releaseActions = releaseActions;
    this.repositoryType = repositoryType;
    this.branchRegex = branchRegex;
    this.gitConnectorId = gitConnectorId;
    this.gitConnectorName = gitConnectorName;
    this.repoName = repoName;
    this.branchName = branchName;
    this.filePaths = filePaths;
    this.checkFileContentChanged = checkFileContentChanged;
    this.webhookSecret = webhookSecret;
  }
}
