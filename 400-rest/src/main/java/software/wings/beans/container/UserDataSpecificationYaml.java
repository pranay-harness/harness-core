package software.wings.beans.container;

import software.wings.beans.DeploymentSpecificationYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class UserDataSpecificationYaml extends DeploymentSpecificationYaml {
  private String data;

  @Builder
  public UserDataSpecificationYaml(String type, String harnessApiVersion, String data) {
    super(type, harnessApiVersion);
    this.data = data;
  }
}
