/**
 *
 */
package software.wings.api;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Rishi
 *
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

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public List<ContextElement> getPartitionElements() {
    return partitionElements;
  }

  public void setPartitionElements(List<ContextElement> partitionElements) {
    this.partitionElements = partitionElements;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ContextElementType getPartitionElementType() {
    if (partitionElements != null && partitionElements.size() > 0) {
      return partitionElements.get(0).getElementType();
    }
    return partitionElementType;
  }

  public void setPartitionElementType(ContextElementType partitionElementType) {
    this.partitionElementType = partitionElementType;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Map<String, Object> paramMap() {
    return null;
  }
}
