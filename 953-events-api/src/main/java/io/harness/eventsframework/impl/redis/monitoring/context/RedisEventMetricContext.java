/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.eventsframework.impl.redis.monitoring.context;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACCOUNT_IDENTIFIER_METRICS_KEY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.STREAM_NAME_METRICS_KEY;
import static io.harness.metrics.MetricConstants.METRIC_LABEL_PREFIX;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.impl.redis.monitoring.dto.RedisEventMetricDTO;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;

@Data
@OwnedBy(HarnessTeam.PL)
@Slf4j
public class RedisEventMetricContext implements AutoCloseable {
  public RedisEventMetricContext(RedisEventMetricDTO redisEventMetricDTO) {
    if (isNotEmpty(redisEventMetricDTO.getAccountId())) {
      ThreadContext.put(METRIC_LABEL_PREFIX + ACCOUNT_IDENTIFIER_METRICS_KEY, redisEventMetricDTO.getAccountId());
    }
    ThreadContext.put(METRIC_LABEL_PREFIX + STREAM_NAME_METRICS_KEY, redisEventMetricDTO.getStreamName());
  }

  private void removeFromContext(Class clazz) {
    Field[] fields = clazz.getDeclaredFields();
    Set<String> names = new HashSet<>();
    for (Field field : fields) {
      names.add(METRIC_LABEL_PREFIX + field.getName());
    }
    ThreadContext.removeAll(names);
  }

  @Override
  public void close() {
    removeFromContext(RedisEventMetricContext.class);
  }
}
