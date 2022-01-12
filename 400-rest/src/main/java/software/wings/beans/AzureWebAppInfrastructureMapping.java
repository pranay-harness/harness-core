/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.validation.Validator.ensureType;
import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.validator.Trimmed;
import io.harness.exception.InvalidRequestException;

import software.wings.api.DeploymentType;
import software.wings.utils.Utils;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.StringUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "AzureWebAppsInfrastructureMappingKeys")
@OwnedBy(CDP)
@TargetModule(_957_CG_BEANS)
public class AzureWebAppInfrastructureMapping extends InfrastructureMapping {
  @Trimmed @Attributes(title = "SubscriptionId") @Getter @Setter private String subscriptionId;
  @Trimmed @Attributes(title = "Resource Group") @Getter @Setter private String resourceGroup;

  public AzureWebAppInfrastructureMapping() {
    super(InfrastructureMappingType.AZURE_WEBAPP.getName());
  }

  @Builder
  public AzureWebAppInfrastructureMapping(String subscriptionId, String resourceGroup) {
    super(InfrastructureMappingType.AZURE_WEBAPP.getName());
    this.subscriptionId = subscriptionId;
    this.resourceGroup = resourceGroup;
  }

  @Override
  public void applyProvisionerVariables(Map<String, Object> map,
      InfrastructureMappingBlueprint.NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
    setAppServiceVariables(map);
  }

  private void setAppServiceVariables(Map<String, Object> map) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      switch (entry.getKey()) {
        case "subscriptionId":
          String errorMsg = "Subscription Id should be of String type";
          notNullCheck(errorMsg, entry.getValue());
          ensureType(String.class, entry.getValue(), errorMsg);
          setSubscriptionId((String) entry.getValue());
          break;

        case "resourceGroup":
          errorMsg = "Resource Group should be of String type";
          notNullCheck(errorMsg, entry.getValue());
          ensureType(String.class, entry.getValue(), errorMsg);
          setResourceGroup((String) entry.getValue());
          break;

        default:
          break;
      }
    }
    validateFieldDefined(subscriptionId, "Subscription Id");
    validateFieldDefined(resourceGroup, "Resource Group");
  }

  protected void validateFieldDefined(String field, String fieldName) {
    if (StringUtils.isEmpty(field)) {
      String message = fieldName + " is required";
      throw new InvalidRequestException(message);
    }
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Utils.normalize(format("(%s)_%s", DeploymentType.AZURE_WEBAPP.name(),
        Optional.ofNullable(getComputeProviderName()).orElse(getComputeProviderType().toLowerCase())));
  }

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return null;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends YamlWithComputeProvider {
    private String subscriptionId;
    private String resourceGroup;

    public Yaml(String type, String harnessApiVersion, String serviceName, String infraMappingType,
        String deploymentType, String computeProviderType, String computeProviderName, Map<String, Object> blueprints,
        String subscriptionId, String resourceGroup) {
      super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, computeProviderType,
          computeProviderName, blueprints);
      this.subscriptionId = subscriptionId;
      this.resourceGroup = resourceGroup;
    }
  }
}
