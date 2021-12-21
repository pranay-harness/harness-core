package io.harness.eventsframework.impl.redis.monitoring.context;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
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
    log.info("in RedisEventMetricContextinside before if block");
    if (isNotEmpty(redisEventMetricDTO.getAccountId())) {
      ThreadContext.put(METRIC_LABEL_PREFIX + "accountId", redisEventMetricDTO.getAccountId());
      log.info("in RedisEventMetricContextinside if block");
    }
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
