/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.eventConfig;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CgEventConfig;
import io.harness.beans.CgEventRule;
import io.harness.beans.WebHookEventConfig;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;
import io.harness.exception.YamlException;
import io.harness.service.EventConfigService;

import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(HarnessTeam.CDC)
public class EventConfigYamlHandler extends BaseYamlHandler<CgEventConfig.Yaml, CgEventConfig> {
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject EventConfigService eventConfigService;
  @Inject YamlHelper yamlHelper;
  @Override
  public void delete(ChangeContext<CgEventConfig.Yaml> changeContext) throws HarnessException {}

  @Override
  public CgEventConfig.Yaml toYaml(CgEventConfig bean, String appId) {
    CgEventRule.PipelineRule pipelineRule = bean.getRule().getPipelineRule();
    CgEventRule.WorkflowRule workflowRule = bean.getRule().getWorkflowRule();
    CgEventRule.PipelineRule.Yaml pipelineYaml = CgEventRule.PipelineRule.Yaml.builder()
                                                     .pipelineIds(pipelineRule.getPipelineIds())
                                                     .allPipelines(pipelineRule.isAllPipelines())
                                                     .allEvents(pipelineRule.isAllEvents())
                                                     .events(pipelineRule.getEvents())
                                                     .build();
    CgEventRule.WorkflowRule.Yaml workflowYaml = CgEventRule.WorkflowRule.Yaml.builder()
                                                     .workflowIds(workflowRule.getWorkflowIds())
                                                     .allWorkflows(workflowRule.isAllWorkflows())
                                                     .allEvents(workflowRule.isAllEvents())
                                                     .events(workflowRule.getEvents())
                                                     .build();
    WebHookEventConfig webHookEventConfig = bean.getConfig();

    return CgEventConfig.Yaml.builder()
        .enabled(bean.isEnabled())
        .type(YamlType.EVENT_RULE.name())
        .cgEventRule(CgEventRule.Yaml.builder()
                         .pipelineRule(pipelineYaml)
                         .workflowRule(workflowYaml)
                         .cgRuleType(bean.getRule().getType())
                         .build())
        .delegateSelectors(bean.getDelegateSelectors())
        .webhookEventConfig(WebHookEventConfig.Yaml.builder()
                                .headers(webHookEventConfig.getHeaders())
                                .url(webHookEventConfig.getUrl())
                                .build())
        .build();
  }

  @Override
  public Class getYamlClass() {
    return CgEventConfig.Yaml.class;
  }

  @Override
  public CgEventConfig upsertFromYaml(
      ChangeContext<CgEventConfig.Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();

    CgEventConfig oldConfig = get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());

    CgEventConfig newConfig = toBean(oldConfig, changeContext, changeSetContext);

    newConfig.setUuid(oldConfig.getUuid());
    newConfig.setAccountId(accountId);

    eventConfigService.updateEventsConfig(accountId, oldConfig.getAppId(), newConfig);
    return newConfig;
  }
  @Override
  public CgEventConfig get(String accountId, String yamlFilePath) {
    return yamlHelper.getEventConfig(accountId, yamlFilePath);
  }

  private CgEventConfig toBean(
      CgEventConfig bean, ChangeContext<CgEventConfig.Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String accountId = changeContext.getChange().getAccountId();
    String entityId = "";

    if (!(changeContext.getChange() instanceof GitFileChange)) {
      throw new YamlException("Error while determining Id for CgEventConfig", WingsException.USER);
    }

    entityId = ((GitFileChange) changeContext.getChange()).getEntityId();

    CgEventConfig.Yaml yaml = changeContext.getYaml();

    bean.setUuid(entityId);
    bean.setAccountId(accountId);
    bean.setEnabled(yaml.isEnabled());
    bean.setDelegateSelectors(yaml.getDelegateSelectors());
    WebHookEventConfig webHookEventConfig = new WebHookEventConfig();
    webHookEventConfig.setUrl(yaml.getWebhookEventConfiguration().getUrl());
    webHookEventConfig.setHeaders(yaml.getWebhookEventConfiguration().getHeaders());
    bean.setConfig(webHookEventConfig);
    CgEventRule.Yaml cgEventRuleYaml = yaml.getEventRule();
    CgEventRule cgEventRule = new CgEventRule();
    CgEventRule.PipelineRule pipelineRule = new CgEventRule.PipelineRule();
    CgEventRule.WorkflowRule workflowRule = new CgEventRule.WorkflowRule();
    if (cgEventRuleYaml.getPipelineRule() != null) {
      pipelineRule.setPipelineIds(cgEventRuleYaml.getPipelineRule().getPipelineIds());
      pipelineRule.setEvents(cgEventRuleYaml.getPipelineRule().getEvents());
      pipelineRule.setAllPipelines(cgEventRuleYaml.getPipelineRule().isAllPipelines());
      pipelineRule.setAllEvents(cgEventRuleYaml.getPipelineRule().isAllEvents());
    }
    if (cgEventRuleYaml.getWorkflowRule() != null) {
      workflowRule.setWorkflowIds(cgEventRuleYaml.getWorkflowRule().getWorkflowIds());
      workflowRule.setEvents(cgEventRuleYaml.getWorkflowRule().getEvents());
      workflowRule.setAllWorkflows(cgEventRuleYaml.getWorkflowRule().isAllWorkflows());
      workflowRule.setAllEvents(cgEventRuleYaml.getWorkflowRule().isAllEvents());
    }
    cgEventRule.setPipelineRule(pipelineRule);
    cgEventRule.setWorkflowRule(workflowRule);
    cgEventRule.setType(cgEventRuleYaml.getRuleType());
    bean.setRule(cgEventRule);
    return bean;
  }
}
