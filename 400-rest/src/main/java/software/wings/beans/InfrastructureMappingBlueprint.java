/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.api.DeploymentType.AMI;
import static software.wings.api.DeploymentType.AWS_LAMBDA;
import static software.wings.api.DeploymentType.ECS;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType.AWS;
import static software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType.GCP;
import static software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType.PHYSICAL_DATA_CENTER;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import software.wings.api.DeploymentType;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@TargetModule(_957_CG_BEANS)
@OwnedBy(CDP)
public class InfrastructureMappingBlueprint {
  public static final String CLOUD_PROVIDER_TYPE_KEY = "cloudProviderType";
  public static final String DEPLOYMENT_TYPE_KEY = "deploymentType";
  public static final String SERVICE_ID_KEY = "serviceId";

  // List of supported from provisioners clouds
  public enum CloudProviderType { AWS, GCP, PHYSICAL_DATA_CENTER }

  // List of possible node filtering done by the blue print
  public enum NodeFilteringType {
    AWS_INSTANCE_FILTER,
    AWS_AUTOSCALING_GROUP,
    AWS_ECS_EC2,
    AWS_ECS_FARGATE,
    AWS_ASG_AMI
  }

  @NotBlank private String serviceId;
  @NotNull private DeploymentType deploymentType;
  @NotNull private CloudProviderType cloudProviderType;
  private NodeFilteringType nodeFilteringType;
  @NotNull @NotEmpty private List<BlueprintProperty> properties;

  private static Map<Pair<DeploymentType, CloudProviderType>, InfrastructureMappingType> infrastructureMappingTypeMap =
      ImmutableMap.<Pair<DeploymentType, CloudProviderType>, InfrastructureMappingType>builder()
          .put(Pair.of(SSH, AWS), InfrastructureMappingType.AWS_SSH)
          .put(Pair.of(ECS, AWS), InfrastructureMappingType.AWS_ECS)
          .put(Pair.of(KUBERNETES, GCP), InfrastructureMappingType.GCP_KUBERNETES)
          .put(Pair.of(AWS_LAMBDA, AWS), InfrastructureMappingType.AWS_AWS_LAMBDA)
          .put(Pair.of(SSH, PHYSICAL_DATA_CENTER), InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH)
          .put(Pair.of(AMI, AWS), InfrastructureMappingType.AWS_AMI)
          .build();

  public static final Map<Pair<DeploymentType, CloudProviderType>, Map<String, String>>
      infrastructureMappingPropertiesMap = ImmutableMap.of(Pair.of(SSH, AWS),
          ImmutableMap.<String, String>builder()
              .put("region", "Region")
              .put("vpcs", "VPCs")
              .put("subnets", "Subnets")
              .put("securityGroups", "Security groups")
              .put("tags", "Tags")
              .build());

  public InfrastructureMappingType infrastructureMappingType() {
    final Pair<DeploymentType, CloudProviderType> typePair = Pair.of(deploymentType, cloudProviderType);
    final InfrastructureMappingType infrastructureMappingType = infrastructureMappingTypeMap.get(typePair);
    if (infrastructureMappingType == null) {
      throw new InvalidRequestException(format("Unsupported deployment %s and cloud provider %s combination.",
          deploymentType.name(), cloudProviderType.name()));
    }

    return infrastructureMappingType;
  }

  @Data
  @Builder
  public static final class Yaml {
    private String serviceName;
    private DeploymentType deploymentType;
    private CloudProviderType cloudProviderType;
    private NodeFilteringType nodeFilteringType;
    private List<BlueprintProperty.Yaml> properties;
  }
}
