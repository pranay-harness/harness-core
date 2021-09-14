/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.type.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.graphql.schema.type.event.QLEventRule.toEventRule;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.CgEventConfig;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLEventsConfig {
  String id;
  private String name;
  private QLWebhookEventConfig webhookConfig;
  private QLEventRule rule;
  private List<String> delegateSelectors;
  private boolean enabled;
  private String appId;

  public static QLEventsConfig getQLEventsConfig(CgEventConfig cgEventConfig) {
    if (cgEventConfig == null) {
      return null;
    }
    QLEventsConfigBuilder builder = QLEventsConfig.builder();
    return builder.appId(cgEventConfig.getAppId())
        .name(cgEventConfig.getName())
        .webhookConfig(QLWebhookEventConfig.toWebhookEventConfig(cgEventConfig.getConfig()))
        .rule(toEventRule(cgEventConfig.getRule()))
        .delegateSelectors(cgEventConfig.getDelegateSelectors())
        .enabled(cgEventConfig.isEnabled())
        .id(cgEventConfig.getUuid())
        .build();
  }
}
