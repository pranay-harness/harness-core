/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.eventsframework.impl.redis.monitoring.dto;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACCOUNT_IDENTIFIER_METRICS_KEY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.producer.Message;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class RedisEventMetricDTOMapper {
  public RedisEventMetricDTO prepareRedisEventMetricDTO(Message message, String streamName) {
    return RedisEventMetricDTO.builder()
        .accountId(message.getMetadataMap().get(ACCOUNT_IDENTIFIER_METRICS_KEY))
        .streamName(streamName)
        .build();
  }
}
