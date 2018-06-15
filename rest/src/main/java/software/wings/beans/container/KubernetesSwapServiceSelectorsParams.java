package software.wings.beans.container;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.service.impl.ContainerServiceParams;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class KubernetesSwapServiceSelectorsParams {
  private String accountId;
  private String appId;
  private String commandName;
  private String activityId;
  private ContainerServiceParams containerServiceParams;
  private String service1;
  private String service2;
}
