package software.wings.verification.appdynamics;

import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import software.wings.verification.CVConfiguration;

@Data
@Builder
@AllArgsConstructor
public class AppDynamicsCVServiceConfiguration extends CVConfiguration {
  @Attributes(required = true, title = "Application Name") private String appDynamicsApplicationId;
  @Attributes(required = true, title = "Tier Name") private String tierId;
}
