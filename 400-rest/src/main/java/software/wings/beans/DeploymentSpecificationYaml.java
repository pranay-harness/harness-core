package software.wings.beans;

import software.wings.yaml.BaseEntityYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public abstract class DeploymentSpecificationYaml extends BaseEntityYaml {
  public DeploymentSpecificationYaml(String type, String harnessApiVersion) {
    super(type, harnessApiVersion);
  }
}
