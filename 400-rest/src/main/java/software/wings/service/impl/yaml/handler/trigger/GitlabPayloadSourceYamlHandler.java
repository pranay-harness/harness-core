/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.trigger.GitLabsPayloadSource;
import software.wings.beans.trigger.PayloadSource;
import software.wings.beans.trigger.WebhookSource.GitLabEventType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.trigger.GitlabPayloadSourceYaml;
import software.wings.yaml.trigger.WebhookEventYaml;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDC)
public class GitlabPayloadSourceYamlHandler extends PayloadSourceYamlHandler<GitlabPayloadSourceYaml> {
  @Override
  public GitlabPayloadSourceYaml toYaml(PayloadSource bean, String appId) {
    GitLabsPayloadSource gitLabsPayloadSource = (GitLabsPayloadSource) bean;

    List<WebhookEventYaml> eventsYaml = new ArrayList<>();
    for (GitLabEventType gitLabEventType : gitLabsPayloadSource.getGitLabEventTypes()) {
      eventsYaml.add(WebhookEventYaml.builder()
                         .eventType(gitLabEventType.getEventType().getValue())
                         .action(gitLabEventType.getValue())
                         .build());
    }
    return GitlabPayloadSourceYaml.builder()
        .customPayloadExpressions(gitLabsPayloadSource.getCustomPayloadExpressions())
        .events(eventsYaml)
        .build();
  }

  @Override
  public PayloadSource upsertFromYaml(
      ChangeContext<GitlabPayloadSourceYaml> changeContext, List<ChangeContext> changeSetContext) {
    GitlabPayloadSourceYaml yaml = changeContext.getYaml();

    List<GitLabEventType> gitLabEvents = new ArrayList<>();
    for (WebhookEventYaml webhookEventYaml : yaml.getEvents()) {
      gitLabEvents.add(GitLabEventType.find(webhookEventYaml.getAction()));
    }
    return GitLabsPayloadSource.builder()
        .customPayloadExpressions(yaml.getCustomPayloadExpressions())
        .gitLabEventTypes(gitLabEvents)
        .build();
  }

  @Override
  public Class getYamlClass() {
    return GitlabPayloadSourceYaml.class;
  }
}
