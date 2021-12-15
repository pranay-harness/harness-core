package io.harness.eventsframework.impl.redis.context;

import static io.harness.metrics.MetricConstants.METRIC_LABEL_PREFIX;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.impl.redis.dto.RedisEventDTO;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import org.apache.logging.log4j.ThreadContext;

@Data
@OwnedBy(HarnessTeam.PL)
public class RedisEventContext implements AutoCloseable {
  private String accountId;

  public RedisEventContext(RedisEventDTO redisEventDTO) {
    this.accountId = redisEventDTO.getAccountId();
    ThreadContext.put(METRIC_LABEL_PREFIX + "accountId", accountId);
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
    removeFromContext(RedisEventContext.class);
  }
}
