/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.event;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.KeyValuePair;
import io.harness.beans.WebHookEventConfig;

import software.wings.graphql.schema.type.QLKeyValuePair;
import software.wings.graphql.schema.type.QLObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QLWebhookEventConfig implements QLObject {
  String url;
  List<QLKeyValuePair> headers;

  public static WebHookEventConfig toWebhookEventConfig(QLWebhookEventConfig config) {
    if (config == null) {
      return null;
    }
    WebHookEventConfig eventConfig = new WebHookEventConfig();
    eventConfig.setUrl(config.getUrl());
    List<KeyValuePair> headers = new ArrayList<>();
    if (isNotEmpty(config.getHeaders())) {
      headers = config.getHeaders()
                    .stream()
                    .map(pair -> KeyValuePair.builder().key(pair.getKey()).value(pair.getValue()).build())
                    .collect(Collectors.toList());
    }
    eventConfig.setHeaders(headers);
    return eventConfig;
  }

  public static QLWebhookEventConfig toWebhookEventConfig(WebHookEventConfig config) {
    if (config == null) {
      return null;
    }
    QLWebhookEventConfig eventConfig = new QLWebhookEventConfig();
    eventConfig.setUrl(config.getUrl());
    List<QLKeyValuePair> headers = new ArrayList<>();
    if (isNotEmpty(config.getHeaders())) {
      headers = config.getHeaders()
                    .stream()
                    .map(pair -> QLKeyValuePair.builder().key(pair.getKey()).value(pair.getValue()).build())
                    .collect(Collectors.toList());
    }
    eventConfig.setHeaders(headers);
    return eventConfig;
  }
}
