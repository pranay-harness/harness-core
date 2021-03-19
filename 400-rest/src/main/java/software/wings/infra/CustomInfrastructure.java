package software.wings.infra;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.expression.Expression;

import software.wings.annotation.IncludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.CustomInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InfrastructureType;
import software.wings.beans.NameValuePair;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName(InfrastructureType.CUSTOM_INFRASTRUCTURE)
public class CustomInfrastructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  public static final String DUMMY_CLOUD_PROVIDER = "DUMMY_CLOUD_PROVIDER";

  @IncludeFieldMap @Expression(ALLOW_SECRETS) private List<NameValuePair> infraVariables;
  @IncludeFieldMap private String deploymentTypeTemplateVersion;

  private transient String customDeploymentName;

  @Override
  public InfrastructureMapping getInfraMapping() {
    final CustomInfrastructureMapping infraMapping =
        CustomInfrastructureMapping.builder().infraVariables(infraVariables).build();
    infraMapping.setInfraMappingType(InfrastructureMappingType.CUSTOM.name());
    infraMapping.setComputeProviderSettingId(DUMMY_CLOUD_PROVIDER);
    infraMapping.setDeploymentTypeTemplateVersion(deploymentTypeTemplateVersion);
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
}
