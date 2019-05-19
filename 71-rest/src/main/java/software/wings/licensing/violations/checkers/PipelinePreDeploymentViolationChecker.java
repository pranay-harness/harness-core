package software.wings.licensing.violations.checkers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.licensing.violations.checkers.ApprovalStepViolationChecker.PIPELINE_APPROVAL_STEP_PREDICATE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.data.structure.CollectionUtils;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.licensing.violations.checkers.error.ValidationError;
import software.wings.service.intfc.WorkflowService;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Singleton
public class PipelinePreDeploymentViolationChecker implements PipelineViolationCheckerMixin {
  public static final String APPROVAL_ERROR_MSG =
      "Pipeline %s is using Professional features (approval integrations, etc.).";
  private static final Collection<RestrictedFeature> RESTRICTED_FEATURES =
      ImmutableList.of(RestrictedFeature.APPROVAL_STEP);

  private WorkflowPreDeploymentViolationChecker workflowPreDeploymentViolationChecker;
  private WorkflowService workflowService;

  @Inject
  public PipelinePreDeploymentViolationChecker(
      WorkflowService workflowService, WorkflowPreDeploymentViolationChecker workflowPreDeploymentViolationChecker) {
    this.workflowService = workflowService;
    this.workflowPreDeploymentViolationChecker = workflowPreDeploymentViolationChecker;
  }

  public List<ValidationError> checkViolations(@NotNull Pipeline pipeline) {
    List<ValidationError> validationErrorList;
    if (isNotEmpty(getPipelineViolationUsages(Lists.newArrayList(pipeline), PIPELINE_APPROVAL_STEP_PREDICATE))) {
      validationErrorList = Lists.newArrayList(
          ValidationError.builder().message(APPROVAL_ERROR_MSG).restrictedFeatures(RESTRICTED_FEATURES).build());
    } else {
      validationErrorList = getPipelineWorkflows(pipeline)
                                .parallelStream()
                                .map(w -> workflowPreDeploymentViolationChecker.checkViolations(w))
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList());
    }
    return CollectionUtils.emptyIfNull(validationErrorList);
  }

  private List<Workflow> getPipelineWorkflows(Pipeline pipeline) {
    List<Workflow> workflowList = null;
    PageRequest<Workflow> workflowPageRequest = getPipelinesPageRequest(pipeline);
    if (workflowPageRequest != null) {
      workflowList = workflowService.listWorkflows(workflowPageRequest).getResponse();
    }

    return CollectionUtils.emptyIfNull(workflowList);
  }
}
