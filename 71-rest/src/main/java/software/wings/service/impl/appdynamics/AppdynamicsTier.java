package software.wings.service.impl.appdynamics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 5/12/17.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"name", "description", "type", "agentType", "externalTiers", "dependencyPath"})
public class AppdynamicsTier {
  private long id;
  private String name;
  private String description;
  private String type;
  private String agentType;
  @Default private Set<AppdynamicsTier> externalTiers = new HashSet<>();
  private String dependencyPath;
}
