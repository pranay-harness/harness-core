package io.harness.limits.counter.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Syncs usage counters for various resources.
 * These counters are checked at the times of resource creation to impose certain limits.
 *
 * A "resource" here refers to Harness entity like service / pipeline / workflow.
 */
@Slf4j
@Singleton
public class CounterSyncer {
  @Inject private CounterService counterService;
  @Inject private AppService appService;
  @Inject private WingsPersistence wingsPersistence;

  public void syncServiceCount(String accountId) {
    if (StringUtils.isEmpty(accountId)) {
      log.error("[service-sync] Invalid args: Empty accountId");
      return;
    }

    Action action = new Action(accountId, ActionType.CREATE_SERVICE);
    Set<String> appIds =
        appService.getAppsByAccountId(accountId).stream().map(Application::getUuid).collect(Collectors.toSet());

    long serviceCount = wingsPersistence.createQuery(Service.class).field(ServiceKeys.appId).in(appIds).count();
    counterService.upsert(new Counter(action.key(), serviceCount));
  }

  public void syncPipelineCount(String accountId) {
    if (StringUtils.isEmpty(accountId)) {
      log.error("[pipeline-sync] Invalid args: Empty accountId");
      return;
    }

    Action action = new Action(accountId, ActionType.CREATE_PIPELINE);
    Set<String> appIds =
        appService.getAppsByAccountId(accountId).stream().map(Application::getUuid).collect(Collectors.toSet());
    long pipelineCount = wingsPersistence.createQuery(Pipeline.class).field(PipelineKeys.appId).in(appIds).count();

    counterService.upsert(new Counter(action.key(), pipelineCount));
  }

  public void syncWorkflowCount(String accountId) {
    if (StringUtils.isEmpty(accountId)) {
      log.error("[workflow-sync] Invalid args: Empty accountId");
      return;
    }

    Action action = new Action(accountId, ActionType.CREATE_WORKFLOW);
    Set<String> appIds =
        appService.getAppsByAccountId(accountId).stream().map(Application::getUuid).collect(Collectors.toSet());
    long workflowCount = wingsPersistence.createQuery(Workflow.class).field(WorkflowKeys.appId).in(appIds).count();

    counterService.upsert(new Counter(action.key(), workflowCount));
  }
}
