package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.yaml.BaseEntityYaml;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class InfrastructureMappingYaml extends BaseEntityYaml {
  private String serviceName;
  private String infraMappingType;
  private String deploymentType;
  private Map<String, Object> blueprints;

  public InfrastructureMappingYaml(String type, String harnessApiVersion, String serviceName, String infraMappingType,
      String deploymentType, Map<String, Object> blueprints) {
    super(type, harnessApiVersion);
    this.serviceName = serviceName;
    this.infraMappingType = infraMappingType;
    this.deploymentType = deploymentType;
    this.blueprints = blueprints;
  }
}
