/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.trigger.TriggerConditionType.WEBHOOK;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.WebHookToken;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by sgurubelli on 10/25/17.
 */

@OwnedBy(CDC)
@TargetModule(HarnessModule._815_CG_TRIGGERS)
@JsonTypeName("WEBHOOK")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class WebHookTriggerCondition extends TriggerCondition {
  public static final String WEBHOOK_SECRET = "webhookSecret";

  private WebHookToken webHookToken;
  private String artifactStreamId;
  @Builder.Default private Map<String, String> parameters = new HashMap<>();
  private WebhookSource webhookSource;
  private List<WebhookEventType> eventTypes;
  private List<GithubAction> actions;
  private List<ReleaseAction> releaseActions;
  private List<BitBucketEventType> bitBucketEvents;
  private List<String> filePaths;
  private String gitConnectorId;
  private String repoName;
  private String branchName;
  private String branchRegex;
  private boolean checkFileContentChanged;
  private String webHookSecret;

  public WebHookTriggerCondition() {
    super(WEBHOOK);
  }

  public WebHookTriggerCondition(WebHookToken webHookToken, String artifactStreamId, Map<String, String> parameters,
      WebhookSource webhookSource, List<WebhookEventType> eventTypes, List<GithubAction> actions,
      List<ReleaseAction> releaseActions, List<BitBucketEventType> bitBucketEvents, List<String> filePaths,
      String gitConnectorId, String repoName, String branchName, String branchRegex, boolean checkFileContentChanged,
      String webHookSecret) {
    this();
    this.webHookToken = webHookToken;
    this.artifactStreamId = artifactStreamId;
    this.parameters = parameters;
    this.webhookSource = webhookSource;
    this.eventTypes = eventTypes;
    this.actions = actions;
    this.releaseActions = releaseActions;
    this.bitBucketEvents = bitBucketEvents;
    this.filePaths = filePaths;
    this.gitConnectorId = gitConnectorId;
    this.repoName = repoName;
    this.branchName = branchName;
    this.branchRegex = branchRegex;
    this.checkFileContentChanged = checkFileContentChanged;
    this.webHookSecret = webHookSecret;
  }
}
