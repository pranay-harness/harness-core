package software.wings.graphql.datafetcher.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.graphql.datafetcher.VariableController;
import software.wings.graphql.schema.query.QLWorkflowVariableQueryParam;
import software.wings.graphql.schema.type.QLVariable;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDC)
@Slf4j
public class WorkflowVariableConnectionDataFetcher
    extends AbstractArrayDataFetcher<QLVariable, QLWorkflowVariableQueryParam> {
  @Inject WorkflowService workflowService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.WORKFLOW, action = PermissionAttribute.Action.READ)
  protected List<QLVariable> fetch(QLWorkflowVariableQueryParam parameters, String accountId) {
    try (AutoLogContext ignore0 = new AccountLogContext(accountId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      String workflowId = parameters.getWorkflowId();
      String appId = parameters.getApplicationId();
      Workflow workflow = workflowService.readWorkflow(appId, workflowId);
      notNullCheck("Workflow " + workflowId + " doesn't exist in the specified application " + appId, workflow, USER);
      notNullCheck(
          "Error reading workflow " + workflowId + " Might be deleted", workflow.getOrchestrationWorkflow(), USER);
      if (!workflow.isTemplatized()) {
        logger.info("No non-fixed variables present in workflow: " + workflowId);
        return new ArrayList<>();
      }
      List<Variable> userVariables = workflow.getOrchestrationWorkflow().getUserVariables();
      List<QLVariable> qlVariables = new ArrayList<>();
      VariableController.populateVariables(userVariables, qlVariables);
      return qlVariables;
    }
  }

  @Override
  protected QLVariable unusedReturnTypePassingDummyMethod() {
    return null;
  }
}
