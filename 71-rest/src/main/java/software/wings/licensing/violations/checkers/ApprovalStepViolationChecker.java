package software.wings.licensing.violations.checkers;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.sm.states.ApprovalState.APPROVAL_STATE_TYPE_VARIABLE;
import static software.wings.sm.states.ApprovalState.USER_GROUPS_VARIABLE;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.data.structure.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AccountType;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureUsageViolation;
import software.wings.beans.FeatureUsageViolation.Usage;
import software.wings.beans.FeatureViolation;
import software.wings.beans.GraphNode;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage.PipelineStageElement;
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
public class ApprovalStepViolationChecker extends AbstractWorkflowViolationChecker {
  private Predicate<GraphNode> workflowApprovalStepPredicate;
  private BiFunction<String, Map<String, Object>, Boolean> approvalStepFunction;
  private Predicate<PipelineStageElement> pipelineApprovalStepPredicate;
  private PipelineService pipelineService;

  @Inject
  public ApprovalStepViolationChecker(WorkflowService workflowService, PipelineService pipelineService) {
    super(workflowService);
    this.pipelineService = pipelineService;
    approvalStepFunction = (t, p)
        -> StateType.APPROVAL.name().equals(t) && isNotEmpty(p)
        && ((p.containsKey(APPROVAL_STATE_TYPE_VARIABLE)
                && !ApprovalStateType.USER_GROUP.name().equals(p.get(APPROVAL_STATE_TYPE_VARIABLE)))
               || !p.containsKey(USER_GROUPS_VARIABLE));

    workflowApprovalStepPredicate = gn -> approvalStepFunction.apply(gn.getType(), gn.getProperties());
    pipelineApprovalStepPredicate = pse -> approvalStepFunction.apply(pse.getType(), pse.getProperties());
  }

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    logger.info("Checking Approval Step violations for accountId={} and targetAccountType={}", accountId,
        AccountType.COMMUNITY);

    List<FeatureViolation> featureViolationList = null;
    List<Usage> workflowViolationUsages = getWorkflowViolationUsages(accountId, workflowApprovalStepPredicate);
    List<Usage> pipelineViolationUsages = getPipelineViolationUsages(accountId, pipelineApprovalStepPredicate);

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

  protected List<Usage> getPipelineViolationUsages(
      @NotNull String accountId, Predicate<PipelineStageElement> graphNodePredicate) {
    List<Usage> flowControlUsages = Lists.newArrayList();

    List<Pipeline> pipelineList = getAllPipelinesByAccountId(accountId);

    pipelineList.stream().filter(p -> isNotEmpty(p.getPipelineStages())).forEach(p -> {
      boolean hasFlowControlViolation =
          p.getPipelineStages()
              .stream()
              .filter(ps -> isNotEmpty(ps.getPipelineStageElements()))
              .anyMatch(ps
                  -> ps.getPipelineStageElements().stream().anyMatch(pse -> pipelineApprovalStepPredicate.test(pse)));

      if (hasFlowControlViolation) {
        flowControlUsages.add(Usage.builder()
                                  .entityId(p.getUuid())
                                  .entityName(p.getName())
                                  .entityType(EntityType.PIPELINE.name())
                                  .build());
      }
    });

    return flowControlUsages;
  }

  protected List<Pipeline> getAllPipelinesByAccountId(@NotNull String accountId) {
    PageRequest<Pipeline> pipelinePageRequest =
        aPageRequest().withLimit(PageRequest.UNLIMITED).addFilter(Pipeline.ACCOUNT_ID_KEY, EQ, accountId).build();
    return pipelineService.listPipelines(pipelinePageRequest);
  }
}
