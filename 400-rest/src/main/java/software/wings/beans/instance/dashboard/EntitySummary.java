package software.wings.beans.instance.dashboard;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * General construct that could be used anywhere
 * @author rktummala on 08/13/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EntitySummary extends AbstractEntitySummary {
  @Builder
  public EntitySummary(String id, String name, String type) {
    super(id, name, type);
  }
}
