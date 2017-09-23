package software.wings.beans.container;

import lombok.Data;
import software.wings.beans.container.ContainerTask.AdvancedType;

/**
 * Created by brett on 9/22/17
 */
@Data
public class ContainerAdvancedPayload {
  private AdvancedType advancedType;
  private String advancedConfig;
}
