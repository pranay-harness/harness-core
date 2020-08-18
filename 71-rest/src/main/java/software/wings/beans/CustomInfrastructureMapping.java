package software.wings.beans;

import static java.lang.String.format;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import software.wings.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@FieldNameConstants(innerTypeName = "CustomInfrastructureMappingKeys")
@Data
@Builder
public class CustomInfrastructureMapping extends InfrastructureMapping {
  private List<NameValuePair> infraVariables;

  @Override
  public void applyProvisionerVariables(Map<String, Object> map,
      InfrastructureMappingBlueprint.NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDefaultName() {
    return Utils.normalize(format("%s (CUSTOM_INFRASTRUCTURE)",
        Optional.ofNullable(getComputeProviderName()).orElse(getComputeProviderType().toLowerCase())));
  }

  @Override
  public String getHostConnectionAttrs() {
    return null;
  }
}
