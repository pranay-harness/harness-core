package software.wings.api;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The type Partition element.
 *
 * @author Rishi
 */
public class PartitionElement implements ContextElement {
  private String uuid;
  private String name;
  private List<ContextElement> partitionElements = new ArrayList<>();
  private ContextElementType partitionElementType;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARTITION;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  /**
   * Sets uuid.
   *
   * @param uuid the uuid
   */
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  /**
   * Gets partition elements.
   *
   * @return the partition elements
   */
  public List<ContextElement> getPartitionElements() {
    return partitionElements;
  }

  /**
   * Sets partition elements.
   *
   * @param partitionElements the partition elements
   */
  public void setPartitionElements(List<ContextElement> partitionElements) {
    this.partitionElements = partitionElements;
  }

  /**
   * Gets partition element type.
   *
   * @return the partition element type
   */
  public ContextElementType getPartitionElementType() {
    if (isNotEmpty(partitionElements)) {
      return partitionElements.get(0).getElementType();
    }
    return partitionElementType;
  }

  /**
   * Sets partition element type.
   *
   * @param partitionElementType the partition element type
   */
  public void setPartitionElementType(ContextElementType partitionElementType) {
    this.partitionElementType = partitionElementType;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
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
