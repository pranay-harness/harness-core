/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.Event.EventCreatorSource;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CgEventConfig;
import io.harness.beans.CgEventRule;
import io.harness.beans.Event;
import io.harness.beans.EventConfig;

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
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
    if (event == null || event.getPayload() == null || event.getPayload().getData() == null
        || !EventCreatorSource.CD.equals(event.getSource())) {
      return false;
    }
    CgEventConfig eventConfig = (CgEventConfig) config;
    CgEventRule.CgRuleType ruleType = eventConfig.getRule().getType();
    if (CgEventRule.CgRuleType.ALL.equals(ruleType)) {
      return true;
    }
    String eventType = event.getPayload().getEventType();
    if (eventType.startsWith("pipeline") && CgEventRule.CgRuleType.PIPELINE.equals(ruleType)) {
      return canSendPipelineEvent(eventConfig, event);
    }
    if (eventType.startsWith("workflow") && CgEventRule.CgRuleType.WORKFLOW.equals(ruleType)) {
      return canSendWorkflowEvent(eventConfig, event);
    }
    return false;
  }

  private static boolean canSendPipelineEvent(CgEventConfig eventConfig, Event event) {
    String eventType = event.getPayload().getEventType();
    CgEventRule.PipelineRule rule = eventConfig.getRule().getPipelineRule();
    // If all events and all pipelines then the event can be sent
    if (rule.isAllEvents() && rule.isAllPipelines()) {
      return true;
    }

    List<String> events = isEmpty(rule.getEvents()) ? Collections.emptyList() : rule.getEvents();
    List<String> pipelineIds = isEmpty(rule.getPipelineIds()) ? Collections.emptyList() : rule.getPipelineIds();

    // We need to check every combination going forward.
    // All pipelines but select events
    if (rule.isAllPipelines() && !rule.isAllEvents()) {
      return events.contains(eventType);
    }

    // All events but select pipelines
    String pipelineId = event.getPayload().getData().getPipelineId();
    if (!rule.isAllPipelines() && rule.isAllEvents()) {
      return pipelineIds.contains(pipelineId);
    }

    // select events and select pipelines
    return events.contains(eventType) && pipelineIds.contains(pipelineId);
  }

  private static boolean canSendWorkflowEvent(CgEventConfig eventConfig, Event event) {
    String eventType = event.getPayload().getEventType();
    CgEventRule.WorkflowRule rule = eventConfig.getRule().getWorkflowRule();
    // If all events and all workflows then the event can be sent
    if (rule.isAllEvents() && rule.isAllWorkflows()) {
      return true;
    }
    List<String> events = isEmpty(rule.getEvents()) ? Collections.emptyList() : rule.getEvents();
    List<String> workflowIds = isEmpty(rule.getWorkflowIds()) ? Collections.emptyList() : rule.getWorkflowIds();

    // We need to check every combination going forward.
    // All workflows but select events
    if (rule.isAllWorkflows() && !rule.isAllEvents()) {
      return events.contains(eventType);
    }

    // All events but select workflows
    String workflowId = event.getPayload().getData().getWorkflowId();
    if (!rule.isAllWorkflows() && rule.isAllEvents()) {
      return workflowIds.contains(event.getPayload().getData().getWorkflowId());
    }

    // select events and select workflows
    return events.contains(eventType) && workflowIds.contains(workflowId);
  }
}
