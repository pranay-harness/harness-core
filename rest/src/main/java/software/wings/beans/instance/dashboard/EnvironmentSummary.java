package software.wings.beans.instance.dashboard;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Artifact information
 * @author rktummala on 08/13/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EnvironmentSummary extends AbstractEntitySummary {
  private boolean prod;

  @Builder
  public EnvironmentSummary(String id, String name, String type, boolean prod) {
    super(id, name, type);
    this.prod = prod;
  }
}
