package software.wings.beans;

import lombok.Builder;
import lombok.Data;

/**
 * Generic entity reference
 * @author rktummala on 12/08/17
 */
@Data
@Builder
public class EntityReference {
  private String id;
  private String name;
  private String appId;
  private String entityType;
}
