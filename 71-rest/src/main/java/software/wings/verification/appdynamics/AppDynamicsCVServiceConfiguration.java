package software.wings.verification.appdynamics;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.verification.CVConfiguration;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppDynamicsCVServiceConfiguration extends CVConfiguration {
  @Attributes(required = true, title = "Application Name") private String appDynamicsApplicationId;
  @Attributes(required = true, title = "Tier Name") private String tierId;

  /**
   * The type Yaml.
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  @AllArgsConstructor
  @Builder
  public static final class AppDynamicsCVConfigurationYaml extends CVConfigurationYaml {
    private String appDynamicsApplicationId;
    private String tierId;

    public AppDynamicsCVConfigurationYaml(String type, String harnessApiVersion, String name, String accountId,
        String connectorId, String envId, String serviceId, String stateType, String analysisTolerance,
        String appDynamicsApplicationId, String tierId) {
      super(type, harnessApiVersion, name, accountId, connectorId, envId, serviceId, stateType, analysisTolerance);
      this.appDynamicsApplicationId = appDynamicsApplicationId;
      this.tierId = tierId;
    }
  }
}
