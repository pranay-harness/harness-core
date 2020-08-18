package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.CloudProviderType;
import software.wings.beans.CustomInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InfrastructureType;
import software.wings.beans.NameValuePair;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import java.util.List;

@Data
@Builder
@JsonTypeName(InfrastructureType.CUSTOM_INFRASTRUCTURE)
public class CustomInfrastructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  public static final String DUMMY_CLOUD_PROVIDER = "DUMMY_CLOUD_PROVIDER";

  private List<NameValuePair> infraVariables;
  private transient String customDeploymentName;

  @Override
  public InfrastructureMapping getInfraMapping() {
    final CustomInfrastructureMapping infraMapping =
        CustomInfrastructureMapping.builder().infraVariables(infraVariables).build();
    infraMapping.setInfraMappingType(InfrastructureMappingType.CUSTOM.name());
    infraMapping.setComputeProviderSettingId(DUMMY_CLOUD_PROVIDER);
    return infraMapping;
  }

  @Override
  public Class<? extends InfrastructureMapping> getMappingClass() {
    return CustomInfrastructureMapping.class;
  }

  @Override
  public String getCloudProviderId() {
    return DUMMY_CLOUD_PROVIDER;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.CUSTOM;
  }

  @Override
  public String getInfrastructureType() {
    return InfrastructureMappingType.CUSTOM.name();
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(InfrastructureType.CUSTOM_INFRASTRUCTURE)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
    private List<NameValuePair> infraVariables;

    @Builder
    public Yaml(String type, List<NameValuePair> infraVariables) {
      super(type);
      setInfraVariables(infraVariables);
    }

    public Yaml() {
      super(InfrastructureType.CUSTOM_INFRASTRUCTURE);
    }
  }
}
