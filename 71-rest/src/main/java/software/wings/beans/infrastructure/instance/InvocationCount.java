package software.wings.beans.infrastructure.instance;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.time.Instant;
import java.util.Objects;

@Value
@Builder
@FieldNameConstants(innerTypeName = "InvocationCountFields")
public class InvocationCount {
  private InvocationCountKey key;
  private long count;
  private Instant from;
  private Instant to;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InvocationCount that = (InvocationCount) o;
    return count == that.count && key == that.key;
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, count);
  }

  public enum InvocationCountKey {
    LAST_30_DAYS,
    SINCE_LAST_DEPLOYED;
  }
}