package software.wings.graphql.datafetcher.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.exception.GraphQLException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.NameValuePair;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.query.QLApproveOrRejectApprovalsInput;
import software.wings.graphql.schema.type.approval.QLApproveOrRejectApprovalsPayload;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.security.auth.DeploymentAuthHandler;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import com.google.inject.Inject;
import graphql.GraphQLContext;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CDC)
public class ApproveOrRejectApprovalsDataFetcher
    extends BaseMutatorDataFetcher<QLApproveOrRejectApprovalsInput, QLApproveOrRejectApprovalsPayload> {
  private WingsPersistence persistence;
  private WorkflowExecutionService workflowExecutionService;
  private DeploymentAuthHandler deploymentAuthHandler;

  @Inject
  public ApproveOrRejectApprovalsDataFetcher(WorkflowExecutionService workflowExecutionService,
      DeploymentAuthHandler deploymentAuthHandler, WingsPersistence persistence) {
    super(QLApproveOrRejectApprovalsInput.class, QLApproveOrRejectApprovalsPayload.class);
    this.workflowExecutionService = workflowExecutionService;
    this.deploymentAuthHandler = deploymentAuthHandler;
    this.persistence = persistence;
  }

  @Override
  @AuthRule(permissionType = LOGGED_IN)
  protected QLApproveOrRejectApprovalsPayload mutateAndFetch(
      QLApproveOrRejectApprovalsInput approveOrRejectApprovalsInput, MutationContext mutationContext) {
    WorkflowExecution execution = persistence.createAuthorizedQuery(WorkflowExecution.class)
                                      .filter("_id", approveOrRejectApprovalsInput.getExecutionId())
                                      .get();
    if (execution == null) {
      throw new InvalidRequestException("Execution does not exist or access is denied", WingsException.USER);
    }

    GraphQLContext graphQLContext = mutationContext.getDataFetchingEnvironment().getContext();
    ApiKeyEntry apiKeyEntry = graphQLContext.get("apiKeyEntry");

    ApprovalDetails approvalDetails = constructApprovalDetailsFromInput(approveOrRejectApprovalsInput, apiKeyEntry);
    ApprovalStateExecutionData approvalStateExecutionData =
        workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
            approveOrRejectApprovalsInput.getApplicationId(), approveOrRejectApprovalsInput.getExecutionId(), null,
            approvalDetails);

    verifyApproveOrRejectApprovalsInput(approveOrRejectApprovalsInput, approvalStateExecutionData);

    if (isEmpty(approvalStateExecutionData.getUserGroups())) {
      deploymentAuthHandler.authorize(
          approveOrRejectApprovalsInput.getApplicationId(), approveOrRejectApprovalsInput.getExecutionId());
    }

    boolean success =
        workflowExecutionService.approveOrRejectExecution(approveOrRejectApprovalsInput.getApplicationId(),
            approvalStateExecutionData.getUserGroups(), approvalDetails, apiKeyEntry);

    return QLApproveOrRejectApprovalsPayload.builder().success(success).build();
  }

  private void verifyApproveOrRejectApprovalsInput(QLApproveOrRejectApprovalsInput approveOrRejectApprovalsInput,
      ApprovalStateExecutionData approvalStateExecutionData) {
    if (!ApprovalStateType.USER_GROUP.equals(approvalStateExecutionData.getApprovalStateType())) {
      throw new GraphQLException(
          approvalStateExecutionData.getApprovalStateType() + " Approval Type not supported", USER);
    }
    if (approveOrRejectApprovalsInput.getVariableInputs() != null) {
      if (approvalStateExecutionData.getVariables() == null) {
        throw new InvalidRequestException("Variable were not present for the given approval");
      }
      approveOrRejectApprovalsInput.getVariableInputs().forEach(nameValuePair -> {
        if (approvalStateExecutionData.getVariables() != null
            && !approvalStateExecutionData.getVariables()
                    .stream()
                    .map(NameValuePair::getName)
                    .collect(Collectors.toSet())
                    .contains(nameValuePair.getName())) {
          throw new InvalidRequestException("Variable with name \"" + nameValuePair.getName() + "\" not present");
        }
      });
    }
  }

  private ApprovalDetails constructApprovalDetailsFromInput(
      QLApproveOrRejectApprovalsInput approveOrRejectApprovalsInput, ApiKeyEntry apiKeyEntry) {
    EmbeddedUser user = null;
    boolean isApprovedViaApiKey = false;
    if (apiKeyEntry != null) {
      user = EmbeddedUser.builder().uuid(apiKeyEntry.getUuid()).name(apiKeyEntry.getName()).build();
      isApprovedViaApiKey = true;
    }

    ApprovalDetails approvalDetails = new ApprovalDetails();
    approvalDetails.setAction(approveOrRejectApprovalsInput.getAction());
    approvalDetails.setApprovalId(approveOrRejectApprovalsInput.getApprovalId());
    approvalDetails.setApprovedBy(user);
    approvalDetails.setComments(approveOrRejectApprovalsInput.getComments());
    approvalDetails.setVariables(approveOrRejectApprovalsInput.getVariableInputs());
    approvalDetails.setApprovalFromGraphQL(true);
    approvalDetails.setApprovalViaApiKey(isApprovedViaApiKey);

    return approvalDetails;
  }
}
