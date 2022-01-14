package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import software.wings.yaml.BaseEntityYaml;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
public abstract class InfraMappingYaml extends BaseEntityYaml {
  private String serviceName;
  private String infraMappingType;
  private String deploymentType;
  private Map<String, Object> blueprints;

  public InfraMappingYaml(String type, String harnessApiVersion, String serviceName, String infraMappingType,
      String deploymentType, Map<String, Object> blueprints) {
    super(type, harnessApiVersion);
    this.serviceName = serviceName;
    this.infraMappingType = infraMappingType;
    this.deploymentType = deploymentType;
    this.blueprints = blueprints;
  }
}
