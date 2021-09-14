/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.beans.dto.eventmapping;

import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.response.TriggerEventResponse;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebhookEventMappingResponse {
  TriggerEventResponse webhookEventResponse;
  ParseWebhookResponse parseWebhookResponse;
  @Default boolean failedToFindTrigger = true;
  boolean isCustomTrigger;
  @Singular List<TriggerDetails> triggers;
}
