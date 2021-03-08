package software.wings.verification.appdynamics;

import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfigurationYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppDynamicsCVServiceConfiguration extends CVConfiguration {
  @Attributes(required = true, title = "Application Name") private String appDynamicsApplicationId;
  @Attributes(required = true, title = "Tier Name") private String tierId;

  @Override
  public CVConfiguration deepCopy() {
    AppDynamicsCVServiceConfiguration clonedConfig = new AppDynamicsCVServiceConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setAppDynamicsApplicationId(this.getAppDynamicsApplicationId());
    clonedConfig.setTierId(this.getTierId());
    return clonedConfig;
  }
}
