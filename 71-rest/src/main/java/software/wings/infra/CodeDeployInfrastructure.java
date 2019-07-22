package software.wings.infra;

import static software.wings.beans.CodeDeployInfrastructureMapping.CodeDeployInfrastructureMappingBuilder.aCodeDeployInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

@JsonTypeName("AWS_AWS_CODEDEPLOY")
@Data
public class CodeDeployInfrastructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  @ExcludeFieldMap private String cloudProviderId;

  private String region;

  @NotEmpty private String applicationName;
  @NotEmpty private String deploymentGroup;
  private String deploymentConfig;
  private String hostNameConvention;

  @Override
  public InfrastructureMapping getInfraMapping() {
    return aCodeDeployInfrastructureMapping()
        .withRegion(region)
        .withApplicationName(applicationName)
        .withDeploymentGroup(deploymentGroup)
        .withDeploymentConfig(deploymentConfig)
        .withHostNameConvention(hostNameConvention)
        .withInfraMappingType(InfrastructureMappingType.AWS_AWS_CODEDEPLOY.name())
        .build();
  }

  @Override
  public Class<CodeDeployInfrastructureMapping> getMappingClass() {
    return CodeDeployInfrastructureMapping.class;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AWS;
  }
}
