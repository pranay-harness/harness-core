/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static java.lang.String.format;

import software.wings.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants(innerTypeName = "CustomInfrastructureMappingKeys")
@Data
@Builder
public class CustomInfrastructureMapping extends InfrastructureMapping {
  private List<NameValuePair> infraVariables;
  private String deploymentTypeTemplateVersion;

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
