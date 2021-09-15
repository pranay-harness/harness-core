/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.settings.helm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ChartMuseumCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HelmInstallationCapability;
import io.harness.expression.ExpressionEvaluator;
import io.harness.k8s.model.HelmVersion;

import software.wings.audit.ResourceType;
import software.wings.security.UsageRestrictions;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;
import software.wings.yaml.setting.HelmRepoYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@JsonTypeName("GCS_HELM_REPO")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._957_CG_BEANS)
public class GCSHelmRepoConfig extends SettingValue implements HelmRepoConfig {
  @SchemaIgnore @NotEmpty private String accountId;

  @NotEmpty private String connectorId;
  @NotEmpty private String bucketName;
  private String folderPath;

  public GCSHelmRepoConfig() {
    super(SettingVariableTypes.GCS_HELM_REPO.name());
  }

  public GCSHelmRepoConfig(String accountId, String connectorId, String bucketName, String folderPath) {
    super(SettingVariableTypes.GCS_HELM_REPO.name());
    this.accountId = accountId;
    this.connectorId = connectorId;
    this.bucketName = bucketName;
    this.folderPath = folderPath;
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.ARTIFACT_SERVER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilityList = new ArrayList<>();
    executionCapabilityList.add(HelmInstallationCapability.builder()
                                    .version(HelmVersion.V3)
                                    .criteria(getType() + ":" + getBucketName())
                                    .build());
    executionCapabilityList.add(ChartMuseumCapability.builder().build());
    return executionCapabilityList;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends HelmRepoYaml {
    private String cloudProvider;
    private String bucket;

    @Builder
    public Yaml(String type, String harnessApiVersion, String cloudProvider, String bucket,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.cloudProvider = cloudProvider;
      this.bucket = bucket;
    }
  }
}
