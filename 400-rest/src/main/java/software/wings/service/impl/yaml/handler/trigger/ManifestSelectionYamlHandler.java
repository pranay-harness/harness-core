/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.validation.Validator.notNullCheck;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.YamlException;

import software.wings.beans.Service;
import software.wings.beans.trigger.ManifestSelection;
import software.wings.beans.trigger.ManifestSelection.ManifestSelectionBuilder;
import software.wings.beans.trigger.ManifestSelection.ManifestSelectionType;
import software.wings.beans.trigger.ManifestSelection.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;

public class ManifestSelectionYamlHandler extends BaseYamlHandler<Yaml, ManifestSelection> {
  @Inject YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;

  @Override
  public void delete(ChangeContext<Yaml> changeContext) {}

  @Override
  public Yaml toYaml(ManifestSelection bean, String appId) {
    return ManifestSelection.Yaml.builder()
        .versionRegex(bean.getVersionRegex())
        .type(bean.getType().name())
        .pipelineName(bean.getPipelineName())
        .serviceName(bean.getServiceName())
        .workflowName(bean.getWorkflowName())
        .build();
  }

  @Override
  public ManifestSelection upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml yaml = changeContext.getYaml();
    String appId =
        yamlHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    String serviceName = yaml.getServiceName();
    Service service;
    if (EmptyPredicate.isNotEmpty(serviceName)) {
      service = serviceResourceService.getServiceByName(appId, serviceName);
      notNullCheck(serviceName + " doesn't exist", service);
    } else {
      throw new InvalidRequestException("Service name cannot be empty.");
    }

    ManifestSelectionBuilder manifestSelectionBuilder = ManifestSelection.builder()
                                                            .serviceId(service.getUuid())
                                                            .serviceName(yaml.getServiceName())
                                                            .type(getType(yaml.getType()))
                                                            .versionRegex(yaml.getVersionRegex());

    String pipelineName = yaml.getPipelineName();
    if (isNotBlank(pipelineName)) {
      manifestSelectionBuilder.pipelineName(pipelineName)
          .pipelineId(yamlHelper.getPipelineFromName(appId, pipelineName).getUuid());
    }

    String workflowName = yaml.getWorkflowName();
    if (isNotBlank(workflowName)) {
      manifestSelectionBuilder.pipelineName(workflowName)
          .pipelineId(yamlHelper.getWorkflowFromName(appId, workflowName).getUuid());
    }

    return manifestSelectionBuilder.build();
  }

  @SuppressWarnings("PMD")
  private ManifestSelectionType getType(String type) {
    try {
      return ManifestSelectionType.valueOf(type);
    } catch (IllegalArgumentException iae) {
      throw new YamlException(
          "The manifest selection type should be one of " + Arrays.toString(ManifestSelectionType.values()), iae,
          WingsException.USER);
    } catch (NullPointerException npe) {
      throw new YamlException("The manifest selection type cannot be empty", npe, WingsException.USER);
    }
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public ManifestSelection get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }
}
