/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.infra;

import static software.wings.beans.CodeDeployInfrastructureMapping.CodeDeployInfrastructureMappingBuilder.aCodeDeployInfrastructureMapping;
import static software.wings.beans.InfrastructureType.CODE_DEPLOY;

import software.wings.annotation.IncludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@JsonTypeName("AWS_AWS_CODEDEPLOY")
@Data
@Builder
public class CodeDeployInfrastructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  private String cloudProviderId;

  @IncludeFieldMap private String region;

  @NotEmpty private String applicationName;
  @NotEmpty private String deploymentGroup;
  private String deploymentConfig;
  private String hostNameConvention;

  @Override
  public InfrastructureMapping getInfraMapping() {
    return aCodeDeployInfrastructureMapping()
        .withComputeProviderSettingId(cloudProviderId)
        .withRegion(region)
        .withComputeProviderSettingId(cloudProviderId)
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
  public String getInfrastructureType() {
    return CODE_DEPLOY;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AWS;
  }
  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(CODE_DEPLOY)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
    private String cloudProviderName;
    private String region;
    @NotEmpty private String applicationName;
    @NotEmpty private String deploymentGroup;
    private String deploymentConfig;
    private String hostNameConvention;

    @Builder
    public Yaml(String type, String cloudProviderName, String region, String applicationName, String deploymentGroup,
        String deploymentConfig, String hostNameConvention) {
      super(type);
      setCloudProviderName(cloudProviderName);
      setRegion(region);
      setApplicationName(applicationName);
      setDeploymentGroup(deploymentGroup);
      setDeploymentConfig(deploymentConfig);
      setHostNameConvention(hostNameConvention);
    }

    public Yaml() {
      super(CODE_DEPLOY);
    }
  }
}
