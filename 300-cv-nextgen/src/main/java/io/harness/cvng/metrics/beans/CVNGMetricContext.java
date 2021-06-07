package io.harness.cvng.metrics.beans;

import static io.harness.cvng.metrics.CVNGMetricsUtils.METRIC_LABEL_PREFIX;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.ThreadContext;

@Data
@Builder
@NoArgsConstructor
public class CVNGMetricContext implements AutoCloseable {
  private String accountId;

  public CVNGMetricContext(String accountId) {
    ThreadContext.put(METRIC_LABEL_PREFIX + "accountId", accountId);
  }

  protected void removeFromContext(Class clazz) {
    Field[] fields = clazz.getDeclaredFields();
    Set<String> names = new HashSet<>();
    for (Field field : fields) {
      names.add(METRIC_LABEL_PREFIX + field.getName());
    }
    ThreadContext.removeAll(names);
  }

  @Override
  public void close() {
    removeFromContext(CVNGMetricContext.class);
  }
}
