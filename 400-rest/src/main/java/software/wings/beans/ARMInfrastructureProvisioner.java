/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.beans.InfrastructureProvisionerType.ARM;

import static org.apache.commons.lang3.StringUtils.trim;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.ARMResourceType;
import io.harness.azure.model.ARMScopeType;
import io.harness.beans.EmbeddedUser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("ARM")
@OwnedBy(CDP)
public class ARMInfrastructureProvisioner extends InfrastructureProvisioner {
  private static String VARIABLE_KEY = "arm";
  private ARMResourceType resourceType;
  private ARMSourceType sourceType;
  private ARMScopeType scopeType;
  private String templateBody;
  private GitFileConfig gitFileConfig;

  @Builder
  private ARMInfrastructureProvisioner(String name, String description, List<NameValuePair> variables,
      List<InfrastructureMappingBlueprint> mappingBlueprints, String accountId, String uuid, String appId,
      EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath,
      ARMSourceType sourceType, String templateBody, GitFileConfig gitFileConfig, ARMScopeType scopeType,
      ARMResourceType resourceType) {
    super(name, description, ARM.name(), variables, mappingBlueprints, accountId, uuid, appId, createdBy, createdAt,
        lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.sourceType = sourceType;
    this.templateBody = trim(templateBody);
    this.gitFileConfig = gitFileConfig;
    this.scopeType = scopeType;
    this.resourceType = resourceType;
  }

  ARMInfrastructureProvisioner() {
    setInfrastructureProvisionerType(ARM.name());
  }

  @Override
  public String variableKey() {
    return VARIABLE_KEY;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static final class Yaml extends InfraProvisionerYaml {
    private ARMResourceType resourceType;
    private ARMSourceType sourceType;
    private String templateBody;
    private GitFileConfig gitFileConfig;
    private ARMScopeType scopeType;

    @Builder
    public Yaml(String type, String harnessApiVersion, String description, List<NameValuePair.Yaml> variables,
        List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints, ARMSourceType sourceType, String templateBody,
        GitFileConfig gitFileConfig, ARMScopeType scopeType, ARMResourceType resourceType) {
      super(type, harnessApiVersion, description, ARM.name(), variables, mappingBlueprints);
      this.sourceType = sourceType;
      this.templateBody = trim(templateBody);
      this.gitFileConfig = gitFileConfig;
      this.scopeType = scopeType;
      this.resourceType = resourceType;
    }
  }
}
