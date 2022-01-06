/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.trigger.BitBucketPayloadSource;
import software.wings.beans.trigger.PayloadSource;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.trigger.BitBucketPayloadSourceYaml;
import software.wings.yaml.trigger.WebhookEventYaml;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDC)
public class BitbucketPayloadSourceYamlHandler extends PayloadSourceYamlHandler<BitBucketPayloadSourceYaml> {
  @Override
  public BitBucketPayloadSourceYaml toYaml(PayloadSource bean, String appId) {
    BitBucketPayloadSource bitBucketPayloadSource = (BitBucketPayloadSource) bean;

    List<WebhookEventYaml> eventsYaml = new ArrayList<>();
    for (BitBucketEventType bitBucketEventType : bitBucketPayloadSource.getBitBucketEvents()) {
      eventsYaml.add(WebhookEventYaml.builder()
                         .eventType(bitBucketEventType.getEventType().getValue())
                         .action(bitBucketEventType.getValue())
                         .build());
    }
    return BitBucketPayloadSourceYaml.builder()
        .customPayloadExpressions(bitBucketPayloadSource.getCustomPayloadExpressions())
        .events(eventsYaml)
        .build();
  }

  @Override
  public PayloadSource upsertFromYaml(
      ChangeContext<BitBucketPayloadSourceYaml> changeContext, List<ChangeContext> changeSetContext) {
    BitBucketPayloadSourceYaml yaml = changeContext.getYaml();

    List<BitBucketEventType> bitBucketEvents = new ArrayList<>();
    for (WebhookEventYaml webhookEventYaml : yaml.getEvents()) {
      bitBucketEvents.add(BitBucketEventType.find(webhookEventYaml.getAction()));
    }
    return BitBucketPayloadSource.builder()
        .customPayloadExpressions(yaml.getCustomPayloadExpressions())
        .bitBucketEvents(bitBucketEvents)
        .build();
  }

  @Override
  public Class getYamlClass() {
    return BitBucketPayloadSourceYaml.class;
  }
}
