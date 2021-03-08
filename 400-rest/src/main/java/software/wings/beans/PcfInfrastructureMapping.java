package software.wings.beans;

import static java.lang.String.format;

import io.harness.beans.EmbeddedUser;

import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.utils.Utils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@JsonTypeName("PCF_PCF")
@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "PcfInfrastructureMappingKeys")
public class PcfInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Organization", required = true) private String organization;
  @Attributes(title = "Space", required = true) private String space;
  @Attributes(title = "Temporary Route Maps") private List<String> tempRouteMap;
  @Attributes(title = "Route Maps", required = true) private List<String> routeMaps;

  /**
   * Instantiates a new Infrastructure mapping.
   */

  public PcfInfrastructureMapping() {
    super(InfrastructureMappingType.PCF_PCF.name());
  }

  @Override
  public void applyProvisionerVariables(
      Map<String, Object> map, NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
    throw new UnsupportedOperationException();
  }

  @Builder
  public PcfInfrastructureMapping(String entityYamlPath, String appId, String accountId, String type, String uuid,
      EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt,
      String computeProviderSettingId, String envId, String serviceTemplateId, String serviceId,
      String computeProviderType, String infraMappingType, String deploymentType, String computeProviderName,
      String name, String organization, String space, List<String> tempRouteMap, List<String> routeMaps,
      String provisionerId, boolean sample) {
    super(entityYamlPath, appId, accountId, type, uuid, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt,
        computeProviderSettingId, envId, serviceTemplateId, serviceId, computeProviderType, infraMappingType,
        deploymentType, computeProviderName, name, true /*autoPopulateName*/, null, provisionerId, sample);
    this.organization = organization;
    this.space = space;
    this.tempRouteMap = tempRouteMap;
    this.routeMaps = routeMaps;
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Utils.normalize(format("%s (%s::%s) %s", this.getOrganization(), this.getComputeProviderType(),
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
        this.getSpace()));
  }

  @Override
  public String getHostConnectionAttrs() {
    return null;
  }
}
