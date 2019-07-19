package software.wings.infra;

import static software.wings.beans.CodeDeployInfrastructureMapping.CodeDeployInfrastructureMappingBuilder.aCodeDeployInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.IncludeInFieldMap;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.CodeDeployInfrastructureMapping.CodeDeployInfrastructureMappingKeys;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

@JsonTypeName("AWS_AWS_CODEDEPLOY")
@Data
public class CodeDeployInfrastructure implements InfraMappingInfrastructureProvider {
  private String cloudProviderId;

  @IncludeInFieldMap(key = CodeDeployInfrastructureMappingKeys.region) private String region;

  @IncludeInFieldMap(key = CodeDeployInfrastructureMappingKeys.applicationName)
  @NotEmpty
  private String applicationName;
  @IncludeInFieldMap(key = CodeDeployInfrastructureMappingKeys.deploymentGroup)
  @NotEmpty
  private String deploymentGroup;
  @IncludeInFieldMap(key = CodeDeployInfrastructureMappingKeys.deploymentConfig) private String deploymentConfig;
  @IncludeInFieldMap(key = CodeDeployInfrastructureMappingKeys.hostNameConvention) private String hostNameConvention;

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
}
