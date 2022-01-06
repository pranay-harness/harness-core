/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.governance.pipeline.service;

import static software.wings.beans.Application.Builder.anApplication;

import static java.util.stream.Collectors.toList;

import io.harness.ff.FeatureFlagService;
import io.harness.governance.pipeline.enforce.GovernanceRuleStatus;
import io.harness.governance.pipeline.enforce.PipelineReportCard;
import io.harness.governance.pipeline.service.evaluators.OnPipeline;
import io.harness.governance.pipeline.service.evaluators.OnWorkflow;
import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig;
import io.harness.governance.pipeline.service.model.PipelineGovernanceRule;
import io.harness.governance.pipeline.service.model.Restriction;
import io.harness.governance.pipeline.service.model.Tag;

import software.wings.beans.HarnessTagLink;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.features.api.Usage;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

@Singleton
public class PipelineGovernanceReportEvaluator {
  @Value
  private static class ReportEvaluationContext {
    private String pipelineId;
    private Pipeline pipeline;
    private List<Workflow> workflows;
  }

  @Inject private PipelineService pipelineService;
  @Inject private WorkflowService workflowService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private HarnessTagService harnessTagService;
  @Inject private PipelineGovernanceService pipelineGovernanceService;
  @Inject @OnPipeline private GovernanceStatusEvaluator<Pipeline> pipelineStatusEvaluator;

  @Inject @OnWorkflow private GovernanceStatusEvaluator<Workflow> workflowStatusEvaluator;

  /**
   * Evaluates how a pipeline performs against a given governance standard.
   */
  private PipelineReportCard getPipelineReportForGovernanceConfig(final String accountId, final String appId,
      final PipelineGovernanceConfig pipelineGovernanceConfig, final ReportEvaluationContext reportEvaluationContext) {
    List<PipelineGovernanceRule> governanceConfigRules = pipelineGovernanceConfig.getRules();

    // this holds the result of each rule
    List<GovernanceRuleStatus> allRulesStatuses = new LinkedList<>();

    for (PipelineGovernanceRule rule : governanceConfigRules) {
      List<GovernanceRuleStatus> tempStatuses = new LinkedList<>();

      GovernanceRuleStatus ruleStatus =
          pipelineStatusEvaluator.status(accountId, reportEvaluationContext.getPipeline(), rule);
      tempStatuses.add(ruleStatus);

      // looks for tags in workflows. If you want to look for tags in some other entity like Commands, then add that
      // logic here by implementing `GovernanceStatusEvaluator` for Command entity and using it here
      for (Workflow workflow : reportEvaluationContext.getWorkflows()) {
        ruleStatus = workflowStatusEvaluator.status(accountId, workflow, rule);
        tempStatuses.add(ruleStatus);
      }

      // one rule (single row of tags) will have one status
      GovernanceRuleStatus finalStatusForRule = merge(rule, tempStatuses);
      allRulesStatuses.add(finalStatusForRule);
    }

    return new PipelineReportCard(pipelineGovernanceConfig, reportEvaluationContext.getPipelineId(), allRulesStatuses);
  }

  private GovernanceRuleStatus merge(final PipelineGovernanceRule rule, final List<GovernanceRuleStatus> statuses) {
    // if tags was found in any resource, we count it as included
    boolean tagsIncluded = statuses.stream().anyMatch(GovernanceRuleStatus::isTagsIncluded);

    // merge all usages into single list
    List<Usage> allUsages =
        statuses.stream().map(GovernanceRuleStatus::getTagsLocations).flatMap(List::stream).collect(toList());

    // final status for given rule
    return new GovernanceRuleStatus(rule.getTags(), rule.getWeight(), tagsIncluded, rule.getMatchType(), allUsages);
  }

  public List<PipelineReportCard> getPipelineReportCard(
      final String accountId, final String appId, final String pipelineId) {
    List<PipelineGovernanceConfig> governanceConfigs =
        pipelineGovernanceService.list(accountId)
            .stream()
            .filter(config -> isConfigValidForApp(accountId, config.getRestrictions(), appId))
            .collect(Collectors.toList());

    governanceConfigs = governanceConfigs.stream().filter(PipelineGovernanceConfig::isEnabled).collect(toList());

    List<PipelineReportCard> pipelineReport = new LinkedList<>();
    Pipeline pipeline = pipelineService.readPipelineWithResolvedVariables(appId, pipelineId, Collections.emptyMap());

    List<Workflow> workflows = pipeline.getWorkflowIds()
                                   .stream()
                                   .map(workflowId -> workflowService.readWorkflow(appId, workflowId))
                                   .collect(toList());

    ReportEvaluationContext context = new ReportEvaluationContext(pipelineId, pipeline, workflows);

    for (PipelineGovernanceConfig config : governanceConfigs) {
      PipelineReportCard reportCard = this.getPipelineReportForGovernanceConfig(accountId, appId, config, context);
      pipelineReport.add(reportCard);
    }

    return pipelineReport;
  }

  @VisibleForTesting
  boolean isConfigValidForApp(final String accountId, final List<Restriction> restrictions, final String appId) {
    // no restrictions
    if (restrictions.isEmpty()) {
      return true;
    }

    for (Restriction restriction : restrictions) {
      if (restriction.getAppIds().contains(appId)) {
        return true;
      }

      if (!restriction.getTags().isEmpty()) {
        List<HarnessTagLink> appTagLinks =
            harnessTagService.fetchTagsForEntity(accountId, anApplication().uuid(appId).build());
        List<Tag> appTags = appTagLinks.stream().map(Tag::fromTagLink).collect(toList());

        return CollectionUtils.containsAll(appTags, restriction.getTags());
      }
    }

    return false;
  }
}
