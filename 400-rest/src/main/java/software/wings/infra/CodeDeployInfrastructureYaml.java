package software.wings.infra;

import static software.wings.beans.InfrastructureType.CODE_DEPLOY;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(CODE_DEPLOY)
public final class CodeDeployInfrastructureYaml extends CloudProviderInfrastructureYaml {
  private String cloudProviderName;
  private String region;
  @NotEmpty private String applicationName;
  @NotEmpty private String deploymentGroup;
  private String deploymentConfig;
  private String hostNameConvention;

  @Builder
  public CodeDeployInfrastructureYaml(String type, String cloudProviderName, String region, String applicationName,
      String deploymentGroup, String deploymentConfig, String hostNameConvention) {
    super(type);
    setCloudProviderName(cloudProviderName);
    setRegion(region);
    setApplicationName(applicationName);
    setDeploymentGroup(deploymentGroup);
    setDeploymentConfig(deploymentConfig);
    setHostNameConvention(hostNameConvention);
  }

  public CodeDeployInfrastructureYaml() {
    super(CODE_DEPLOY);
  }
}
