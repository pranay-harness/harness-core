package software.wings.beans;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonTypeName("PCF_PCF")
@Data
@EqualsAndHashCode(callSuper = true)
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
  public void applyProvisionerVariables(Map<String, Object> map, NodeFilteringType nodeFilteringType) {}

  @Builder
  public PcfInfrastructureMapping(String entityYamlPath, String appId, String accountId, String type, String uuid,
      EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt,
      String computeProviderSettingId, String envId, String serviceTemplateId, String serviceId,
      String computeProviderType, String infraMappingType, String deploymentType, String computeProviderName,
      String name, String organization, String space, List<String> tempRouteMap, List<String> routeMaps) {
    super(entityYamlPath, appId, accountId, type, uuid, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt,
        computeProviderSettingId, envId, serviceTemplateId, serviceId, computeProviderType, infraMappingType,
        deploymentType, computeProviderName, name, true /*autoPopulateName*/, null);
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

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends YamlWithComputeProvider {
    private String organization;
    private String space;
    private List<String> tempRouteMap;
    private List<String> routeMaps;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String organization, String space,
        List<String> tempRouteMap, List<String> routeMaps, Map<String, Object> blueprints) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName, blueprints);
      this.organization = organization;
      this.space = space;
      this.tempRouteMap = tempRouteMap;
      this.routeMaps = routeMaps;
    }
  }
}
