package io.harness.governance.pipeline.service;

import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.governance.pipeline.enforce.GovernanceRuleStatus;
import io.harness.governance.pipeline.enforce.PipelineReportCard;
import io.harness.governance.pipeline.model.PipelineGovernanceConfig;
import io.harness.governance.pipeline.model.PipelineGovernanceRule;
import io.harness.governance.pipeline.service.evaluators.OnPipeline;
import io.harness.governance.pipeline.service.evaluators.OnWorkflow;
import lombok.Value;
import software.wings.beans.HarnessTag;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.features.api.Usage;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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

  @Inject private HarnessTagService harnessTagService;
  @Inject private PipelineGovernanceService pipelineGovernanceService;
  @Inject @OnPipeline private GovernanceStatusEvaluator<Pipeline> pipelineStatusEvaluator;

  @Inject @OnWorkflow private GovernanceStatusEvaluator<Workflow> workflowStatusEvaluator;

  /**
   * Evaluates how a pipeline performs against a given governance standard.
   */
  private PipelineReportCard getPipelineReportForGovernanceConfig(final String accountId, final String appId,
      final PipelineGovernanceConfig pipelineGovernanceConfig, final ReportEvaluationContext reportEvaluationContext) {
    List<String> accountTagIds =
        harnessTagService.listTags(accountId).stream().map(HarnessTag::getUuid).collect(toList());

    List<PipelineGovernanceRule> governanceConfigRules = pipelineGovernanceConfig.getRules();

    // this holds the result of each rule
    List<GovernanceRuleStatus> allRulesStatuses = new LinkedList<>();

    for (PipelineGovernanceRule rule : governanceConfigRules) {
      List<GovernanceRuleStatus> tempStatuses = new LinkedList<>();

      GovernanceRuleStatus ruleStatus =
          pipelineStatusEvaluator.status(accountId, reportEvaluationContext.getPipeline(), rule);
      tempStatuses.add(ruleStatus);

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
    List<PipelineGovernanceConfig> governanceConfigs = pipelineGovernanceService.list(accountId)
                                                           .stream()
                                                           .filter(config -> isConfigValidForApp(config, appId))
                                                           .collect(Collectors.toList());

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

  private boolean isConfigValidForApp(final PipelineGovernanceConfig config, final String appId) {
    if (!config.getAppIds().isEmpty()) {
      return config.getAppIds().contains(appId);
    } else {
      return true;
    }
  }
}
