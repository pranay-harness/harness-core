package software.wings.licensing.violations.checkers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.sm.states.ApprovalState.APPROVAL_STATE_TYPE_VARIABLE;
import static software.wings.sm.states.ApprovalState.USER_GROUPS_VARIABLE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureUsageViolation;
import software.wings.beans.FeatureUsageViolation.Usage;
import software.wings.beans.FeatureViolation;
import software.wings.beans.GraphNode;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Workflow;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;

@Singleton
@Slf4j
public class ApprovalStepViolationChecker
    implements FeatureViolationChecker, WorkflowViolationCheckerMixin, PipelineViolationCheckerMixin {
  private static final BiFunction<String, Map<String, Object>, Boolean> APPROVAL_STEP_FUNCTION = (t, p)
      -> StateType.APPROVAL.name().equals(t) && isNotEmpty(p)
      && ((p.containsKey(APPROVAL_STATE_TYPE_VARIABLE)
              && !ApprovalStateType.USER_GROUP.name().equals(p.get(APPROVAL_STATE_TYPE_VARIABLE)))
             || !p.containsKey(USER_GROUPS_VARIABLE));

  public static final Predicate<GraphNode> WORKFLOW_APPROVAL_STEP_PREDICATE =
      gn -> APPROVAL_STEP_FUNCTION.apply(gn.getType(), gn.getProperties());
  public static final Predicate<PipelineStageElement> PIPELINE_APPROVAL_STEP_PREDICATE =
      pse -> APPROVAL_STEP_FUNCTION.apply(pse.getType(), pse.getProperties());

  private PipelineService pipelineService;
  private WorkflowService workflowService;

  @Inject
  public ApprovalStepViolationChecker(WorkflowService workflowService, PipelineService pipelineService) {
    this.workflowService = workflowService;
    this.pipelineService = pipelineService;
  }

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    logger.info("Checking Approval Step violations for accountId={} and targetAccountType={}", accountId,
        AccountType.COMMUNITY);

    List<FeatureViolation> featureViolationList = null;
    List<Usage> workflowViolationUsages =
        getWorkflowViolationUsages(getAllWorkflowsByAccountId(accountId), WORKFLOW_APPROVAL_STEP_PREDICATE);
    List<Usage> pipelineViolationUsages =
        getPipelineViolationUsages(getAllPipelinesByAccountId(accountId), PIPELINE_APPROVAL_STEP_PREDICATE);

    List<Usage> combinedUsages = Stream.of(workflowViolationUsages, pipelineViolationUsages)
                                     .flatMap(Collection::stream)
                                     .collect(Collectors.toList());

    if (isNotEmpty(combinedUsages)) {
      logger.info("Found {} Approval Steps violations for accountId={} and targetAccountType={}", combinedUsages.size(),
          accountId, AccountType.COMMUNITY);
      featureViolationList = Collections.singletonList(FeatureUsageViolation.builder()
                                                           .restrictedFeature(RestrictedFeature.APPROVAL_STEP)
                                                           .usages(combinedUsages)
                                                           .build());
    }

    return CollectionUtils.emptyIfNull(featureViolationList);
  }

  protected List<Pipeline> getAllPipelinesByAccountId(@NotNull String accountId) {
    return pipelineService.listPipelines(getPipelinesPageRequest(accountId));
  }

  private List<Workflow> getAllWorkflowsByAccountId(@NotNull String accountId) {
    return workflowService.listWorkflows(getWorkflowsPageRequest(accountId)).getResponse();
  }
}
