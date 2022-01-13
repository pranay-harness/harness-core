/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.template;

import static software.wings.common.TemplateConstants.ARTIFACT_SOURCE;
import static software.wings.common.TemplateConstants.HTTP;
import static software.wings.common.TemplateConstants.PCF_PLUGIN;
import static software.wings.common.TemplateConstants.SHELL_SCRIPT;
import static software.wings.common.TemplateConstants.SSH;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import software.wings.beans.template.artifactsource.ArtifactSourceTemplate;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.beans.template.command.PcfCommandTemplate;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.beans.template.deploymenttype.CustomDeploymentTypeTemplate;
import software.wings.common.TemplateConstants;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = SshCommandTemplate.class, name = SSH)
  , @JsonSubTypes.Type(value = HttpTemplate.class, name = HTTP),
      @JsonSubTypes.Type(value = ShellScriptTemplate.class, name = SHELL_SCRIPT),
      @JsonSubTypes.Type(value = ArtifactSourceTemplate.class, name = ARTIFACT_SOURCE),
      @JsonSubTypes.Type(value = PcfCommandTemplate.class, name = PCF_PLUGIN),
      @JsonSubTypes.Type(value = CustomDeploymentTypeTemplate.class, name = TemplateConstants.CUSTOM_DEPLOYMENT_TYPE)
})
public interface BaseTemplate {}
