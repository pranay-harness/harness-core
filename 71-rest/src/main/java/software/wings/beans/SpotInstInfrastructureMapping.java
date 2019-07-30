package software.wings.beans;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.exception.InvalidRequestException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import software.wings.annotation.Blueprint;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;

import java.util.Map;

@JsonTypeName("SpotInst")
@FieldNameConstants(innerTypeName = "SpotInstInfrastructureMappingKeys")
public class SpotInstInfrastructureMapping extends InfrastructureMapping {
  @Blueprint @Getter @Setter private String awsRegion;
  @Blueprint @Getter @Setter private String elasticGroupJson;
  @Blueprint @Getter @Setter private String spotinstConnectorId;

  public SpotInstInfrastructureMapping() {
    super(InfrastructureMappingType.SPOTINST.name());
  }

  @Override
  public void applyProvisionerVariables(
      Map<String, Object> map, NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
    throw new InvalidRequestException(format("Unidentified: [%s] node filtering type", nodeFilteringType.name()));
  }

  @Override
  public String getDefaultName() {
    return null;
  }

  @Override
  public String getHostConnectionAttrs() {
    return null;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends YamlWithComputeProvider {
    private String elasticGroupJson;
    private String spotinstConnectorId;
    private String awsRegion;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, Map<String, Object> blueprints,
        String elasticGroupJson, String spotinstConnectorId, String awsRegion) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName, blueprints);
      this.elasticGroupJson = elasticGroupJson;
      this.spotinstConnectorId = spotinstConnectorId;
      this.awsRegion = awsRegion;
    }
  }
}
