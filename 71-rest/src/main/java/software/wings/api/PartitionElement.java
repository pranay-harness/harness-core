package software.wings.api;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.context.ContextElementType;
import lombok.Builder;
import lombok.Value;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * The type Partition element.
 */
@Value
@Builder
public class PartitionElement implements ContextElement {
  private String uuid;
  private String name;
  private List<ContextElement> partitionElements;
  private ContextElementType partitionElementType;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARTITION;
  }

  public ContextElementType getPartitionElementType() {
    if (isNotEmpty(partitionElements)) {
      return partitionElements.get(0).getElementType();
    }
    return partitionElementType;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }
}
