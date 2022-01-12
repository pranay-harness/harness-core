/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.templatelibrary;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.common.TemplateConstants.CUSTOM;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.template.artifactsource.CustomRepositoryMapping;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("CUSTOM")
@JsonPropertyOrder({"harnessApiVersion"})
public class CustomArtifactSourceTemplateYaml extends ArtifactSourceTemplateYaml {
  private String script;
  private String timeout;
  private CustomRepositoryMapping customRepositoryMapping;

  @Builder
  public CustomArtifactSourceTemplateYaml(String script, String timeout,
      CustomRepositoryMapping customRepositoryMapping, String type, String harnessApiVersion, String description,
      List<TemplateVariableYaml> templateVariableYamlList) {
    super(type, harnessApiVersion, description, templateVariableYamlList, CUSTOM);
    this.script = script;
    this.customRepositoryMapping = customRepositoryMapping;
    this.timeout = timeout;
  }

  public CustomArtifactSourceTemplateYaml() {
    super(CUSTOM);
  }
}
