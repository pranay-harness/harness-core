package software.wings.beans.instance.dashboard;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Service info with parent app info
 * @author rktummala on 08/13/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceSummary extends AbstractEntitySummary {
  private EntitySummary appSummary;

  @Builder
  public ServiceSummary(String id, String name, String type, EntitySummary appSummary) {
    super(id, name, type);
    this.appSummary = appSummary;
  }
}
