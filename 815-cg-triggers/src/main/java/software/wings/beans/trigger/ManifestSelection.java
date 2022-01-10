/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.BaseYaml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "ManifestSelectionKeys")
public class ManifestSelection {
  @NotEmpty private String serviceId;
  private String serviceName;
  @NotEmpty private ManifestSelectionType type;
  private String appManifestId;
  private String versionRegex;
  private String pipelineId;
  private String pipelineName;
  private String workflowId;
  private String workflowName;
  private String appManifestName;

  public enum ManifestSelectionType {
    FROM_APP_MANIFEST,
    LAST_COLLECTED,
    LAST_DEPLOYED,
    PIPELINE_SOURCE,
    WEBHOOK_VARIABLE
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseYaml {
    String type;
    private String versionRegex;
    String workflowName;
    String pipelineName;
    String serviceName;

    @lombok.Builder
    public Yaml(String type, String workflowName, String versionRegex, String serviceName, String pipelineName) {
      this.workflowName = workflowName;
      this.pipelineName = pipelineName;
      this.versionRegex = versionRegex;
      this.type = type;
      this.serviceName = serviceName;
    }
  }
}
