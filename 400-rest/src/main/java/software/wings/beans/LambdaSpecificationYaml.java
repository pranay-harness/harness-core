package software.wings.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class LambdaSpecificationYaml extends DeploymentSpecificationYaml {
  private LambdaSpecificationDefaultSpecificationYaml defaults;
  private List<LambdaSpecificationsYaml> functions;

  @Builder
  public LambdaSpecificationYaml(String type, String harnessApiVersion,
      LambdaSpecificationDefaultSpecificationYaml defaults, List<LambdaSpecificationsYaml> functions) {
    super(type, harnessApiVersion);
    this.defaults = defaults;
    this.functions = functions;
  }
}
