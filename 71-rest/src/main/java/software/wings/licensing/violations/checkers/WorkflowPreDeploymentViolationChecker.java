package software.wings.licensing.violations.checkers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageResponse;
import io.harness.data.structure.CollectionUtils;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.licensing.violations.checkers.error.ValidationError;
import software.wings.service.intfc.ServiceResourceService;

import java.util.Collection;
import java.util.List;
import javax.validation.constraints.NotNull;

@Singleton
public class WorkflowPreDeploymentViolationChecker
    implements WorkflowViolationCheckerMixin, ServiceViolationCheckerMixin {
  public static final String WORKFLOW_RESTRICTED_FEATURE_ERROR_MSG =
      "Workflow %s is using Professional features (Flow controls, approval integrations, templates, etc.).";

  public static final String WORKFLOW_SERVICE_RESTRICTED_FEATURE_ERROR_MSG =
      "A Service in Workflow %s is using Professional features (Flow controls, approval integrations, templates, etc.).";

  public static final Collection<RestrictedFeature> WORKFLOW_RESTRICTED_FEATURES =
      ImmutableList.of(RestrictedFeature.APPROVAL_STEP, RestrictedFeature.FLOW_CONTROL);

  private ServiceResourceService serviceResourceService;

  @Inject
  public WorkflowPreDeploymentViolationChecker(ServiceResourceService serviceResourceService) {
    this.serviceResourceService = serviceResourceService;
  }

  public List<ValidationError> checkViolations(@NotNull Workflow workflow) {
    List<ValidationError> validationErrorList = null;
    if (WORFKFLOW_TEMPLAE_USAGE_PREDICATE.test(workflow)
        || isNotEmpty(
               getWorkflowViolationUsages(Lists.newArrayList(workflow), WORKFLOW_COMMUNITY_VIOLATION_PREDICATE))) {
      validationErrorList = Lists.newArrayList(ValidationError.builder()
                                                   .message(getWorkflowRestrictedFeatureErrorMsg(workflow.getName()))
                                                   .restrictedFeatures(WORKFLOW_RESTRICTED_FEATURES)
                                                   .build());
    } else if (isNotEmpty(getViolationsInServices(getServicesForAccount(
                   workflow.getAccountId(), workflow.getOrchestrationWorkflow().getServiceIds())))) {
      validationErrorList =
          Lists.newArrayList(ValidationError.builder()
                                 .message(getWorkflowServiceRestrictedFeatureErrorMsg(workflow.getName()))
                                 .restrictedFeatures(SERVICE_RESTRICTED_FEATURES)
                                 .build());
    }
    return CollectionUtils.emptyIfNull(validationErrorList);
  }

  public static String getWorkflowRestrictedFeatureErrorMsg(String workflowName) {
    return String.format(WORKFLOW_RESTRICTED_FEATURE_ERROR_MSG, workflowName);
  }

  public static String getWorkflowServiceRestrictedFeatureErrorMsg(String workflowName) {
    return String.format(WORKFLOW_SERVICE_RESTRICTED_FEATURE_ERROR_MSG, workflowName);
  }

  private PageResponse<Service> getServicesForAccount(@NotNull String accountId, List<String> serviceIdList) {
    return serviceResourceService.list(getServicePageRequest(accountId, serviceIdList), false, true);
  }
}
