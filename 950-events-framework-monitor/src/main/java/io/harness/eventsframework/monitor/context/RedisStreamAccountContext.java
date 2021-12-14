package io.harness.eventsframework.monitor.context;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.monitor.dto.RedisStreamAccountDTO;
import lombok.Data;
import org.apache.logging.log4j.ThreadContext;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static io.harness.metrics.MetricConstants.METRIC_LABEL_PREFIX;

@Data
@OwnedBy(HarnessTeam.PL)
public class RedisStreamAccountContext implements AutoCloseable{
    private String accountId;

    public RedisStreamAccountContext(RedisStreamAccountDTO redisStreamAccountDTO){
        this.accountId=redisStreamAccountDTO.getAccountId();
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
        removeFromContext(RedisStreamContext.class);
    }
}
