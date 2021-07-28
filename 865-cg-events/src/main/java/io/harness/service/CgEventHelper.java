package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.Event.EventCreatorSource;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CgEventConfig;
import io.harness.beans.CgEventRule;
import io.harness.beans.Event;
import io.harness.beans.EventConfig;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class CgEventHelper implements EventHelper {
  @Override
  public boolean canSendEvent(EventConfig config, Event event, String appId) {
    if (!(config instanceof CgEventConfig)) {
      return false;
    }
    if (event == null || event.getPayload() == null || !EventCreatorSource.CD.equals(event.getSource())) {
      return false;
    }
    CgEventConfig eventConfig = (CgEventConfig) config;
    CgEventRule rule = eventConfig.getRule();
    CgEventRule.CgRuleType ruleType = rule.getType();
    if (CgEventRule.CgRuleType.ALL.equals(ruleType)) {
      return true;
    }
    String eventType = event.getPayload().getEventType();
    if (eventType.startsWith("pipeline") && CgEventRule.CgRuleType.PIPELINE.equals(ruleType)) {
      CgEventRule.PipelineRule pipelineRule = rule.getPipelineRule();
      if (pipelineRule.isAllEvents() && pipelineRule.isAllPipelines()) {
        return true;
      }
      // TODO: Handle other cases
      return false;
    }
    if (eventType.startsWith("workflow") && CgEventRule.CgRuleType.PIPELINE.equals(ruleType)) {
      CgEventRule.WorkflowRule workflowRule = rule.getWorkflowRule();
      if (workflowRule.isAllEvents() && workflowRule.isAllWorkflows()) {
        return true;
      }
      // TODO: Handle other cases
      return false;
    }
    return false;
  }
}
