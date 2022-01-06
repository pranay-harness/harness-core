/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.eventsframework.monitor.context;

import static io.harness.metrics.MetricConstants.METRIC_LABEL_PREFIX;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.monitor.dto.RedisStreamDTO;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import org.apache.logging.log4j.ThreadContext;

@Data
@OwnedBy(HarnessTeam.PL)
public class RedisStreamContext implements AutoCloseable {
  private String name;
  private String usecaseName;
  private String namespace;

  public RedisStreamContext(RedisStreamDTO redisStreamDTO) {
    name = redisStreamDTO.getStreamName();
    usecaseName = redisStreamDTO.getUsecaseName();
    namespace = redisStreamDTO.getNamespace();
    ThreadContext.put(METRIC_LABEL_PREFIX + "name", name);
    ThreadContext.put(METRIC_LABEL_PREFIX + "usecaseName", usecaseName);
    ThreadContext.put(METRIC_LABEL_PREFIX + "namespace", namespace);
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
    removeFromContext(RedisStreamContext.class);
  }
}
