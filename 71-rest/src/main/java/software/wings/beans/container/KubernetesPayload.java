package software.wings.beans.container;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Created by brett on 9/22/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class KubernetesPayload {
  private String advancedConfig;
}
