package software.wings.beans;

import static java.lang.String.format;
import static software.wings.api.DeploymentType.ECS;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType.AWS;
import static software.wings.beans.InfrastructureMappingType.AWS_ECS;

import com.google.common.collect.ImmutableMap;

import io.harness.exception.InvalidRequestException;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.api.DeploymentType;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Data
@Builder
public class InfrastructureMappingBlueprint {
  public static final String CLOUD_PROVIDER_TYPE_KEY = "cloudProviderType";
  public static final String DEPLOYMENT_TYPE_KEY = "deploymentType";
  public static final String SERVICE_ID_KEY = "serviceId";

  // List of supported from provisioners clouds
  public enum CloudProviderType { AWS }

  // List of possible node filtering done by the blue print
  public enum NodeFilteringType { AWS_INSTANCE_FILTER, AWS_AUTOSCALING_GROUP, AWS_ECS_EC2 }

  @NotBlank private String serviceId;
  @NotNull private DeploymentType deploymentType;
  @NotNull private CloudProviderType cloudProviderType;
  @NotNull private NodeFilteringType nodeFilteringType;
  @NotNull @NotEmpty private List<NameValuePair> properties;

  private static Map<Pair<DeploymentType, CloudProviderType>, InfrastructureMappingType> infrastructureMappingTypeMap =
      ImmutableMap.of(Pair.of(SSH, AWS), InfrastructureMappingType.AWS_SSH, Pair.of(ECS, AWS), AWS_ECS);

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

  @Value
  @Builder
  @Data
  public static final class Yaml {
    private String serviceName;
    private DeploymentType deploymentType;
    private CloudProviderType cloudProviderType;
    private NodeFilteringType nodeFilteringType;
    private List<NameValuePair.Yaml> properties;
  }
}
