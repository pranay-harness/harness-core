/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.trigger.ArtifactSelection.Type.ARTIFACT_SOURCE;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_COLLECTED;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_DEPLOYED;
import static software.wings.beans.trigger.ArtifactSelection.Type.PIPELINE_SOURCE;
import static software.wings.beans.trigger.ArtifactSelection.Type.WEBHOOK_VARIABLE;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.GeneralException;
import io.harness.exception.WingsException;

import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactSelection.Type;
import software.wings.beans.trigger.ArtifactSelection.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(CDC)
public class ArtifactSelectionYamlHandler extends BaseYamlHandler<Yaml, ArtifactSelection> {
  @Inject protected YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;

  @Override
  public void delete(ChangeContext<Yaml> changeContext) {}

  @Override
  public Yaml toYaml(ArtifactSelection bean, String appId) {
    ArtifactSelection.Yaml yaml = ArtifactSelection.Yaml.builder()
                                      .artifactFilter(bean.getArtifactFilter())
                                      .type(bean.getType().name())
                                      .serviceName(bean.getServiceName())
                                      .workflowName(bean.getWorkflowName())
                                      .pipelineName(bean.getPipelineName())
                                      .build();

    if (EmptyPredicate.isNotEmpty(bean.getArtifactStreamId())) {
      yaml.setArtifactStreamName(yamlHelper.getArtifactStreamName(appId, bean.getArtifactStreamId()));
    }

    if (bean.getType() == LAST_COLLECTED) {
      yaml.setRegex(bean.isRegex());
    }

    return yaml;
  }

  @Override
  public ArtifactSelection upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    ArtifactSelection.Yaml yaml = changeContext.getYaml();
    String appId =
        yamlHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    String serviceName = yaml.getServiceName();
    String artifactStreamName = yaml.getArtifactStreamName();
    ArtifactStream artifactStream = null;
    if (EmptyPredicate.isNotEmpty(artifactStreamName) && EmptyPredicate.isNotEmpty(serviceName)) {
      artifactStream = yamlHelper.getArtifactStreamWithName(appId, serviceName, artifactStreamName);
    }
    String serviceId = null;
    if (EmptyPredicate.isNotEmpty(serviceName)) {
      Service service = serviceResourceService.getServiceByName(appId, serviceName);
      notNullCheck(serviceName + " doesnot exist", service);
      serviceId = service.getUuid();
    } else {
      throw new GeneralException("Service name cannot be empty.");
    }

    ArtifactSelection artifactSelection = ArtifactSelection.builder()
                                              .artifactFilter(yaml.getArtifactFilter())
                                              .regex(yaml.isRegex())
                                              .type(getArtifactSelectionType(yaml.getType()))
                                              .serviceId(serviceId)
                                              .serviceName(serviceName)
                                              .build();

    if (isNotBlank(yaml.getPipelineName())) {
      String pipelineId = yamlHelper.getPipelineFromName(appId, yaml.getPipelineName()).getUuid();
      artifactSelection.setPipelineId(pipelineId);
      artifactSelection.setPipelineName(yaml.getPipelineName());
    } else if (isNotBlank(yaml.getWorkflowName())) {
      Workflow workflow = yamlHelper.getWorkflowFromName(appId, yaml.getWorkflowName());
      artifactSelection.setWorkflowName(workflow.getName());
      artifactSelection.setWorkflowId(workflow.getUuid());
    }

    if (artifactStream != null) {
      artifactSelection.setArtifactSourceName(artifactStream.generateSourceName());
      artifactSelection.setArtifactStreamId(artifactStream.getUuid());
    }

    return artifactSelection;
  }

  @Override
  public Class getYamlClass() {
    return ArtifactSelection.Yaml.class;
  }

  @Override
  public ArtifactSelection get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  private Type getArtifactSelectionType(String type) {
    if (EmptyPredicate.isEmpty(type)) {
      return null;
    }

    if (type.equals(ARTIFACT_SOURCE.name())) {
      return ARTIFACT_SOURCE;
    } else if (type.equals(WEBHOOK_VARIABLE.name())) {
      return WEBHOOK_VARIABLE;
    } else if (type.equals(PIPELINE_SOURCE.name())) {
      return PIPELINE_SOURCE;
    } else if (type.equals(LAST_COLLECTED.name())) {
      return LAST_COLLECTED;
    } else if (type.equals(LAST_DEPLOYED.name())) {
      return LAST_DEPLOYED;
    } else {
      notNullCheck("Invalid type", type);
      return null;
    }
  }
}
