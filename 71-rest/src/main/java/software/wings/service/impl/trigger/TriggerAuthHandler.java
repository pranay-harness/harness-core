package software.wings.service.impl.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import software.wings.beans.Environment;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;

import java.util.List;

@OwnedBy(CDC)
@Singleton
public class TriggerAuthHandler {
  @Inject private AuthHandler authHandler;
  @Inject private EnvironmentService environmentService;
  @Inject private AuthService authService;
  @Inject private PipelineService pipelineService;
  @Inject private WorkflowService workflowService;

  void authorizeEnvironment(String appId, String environmentValue) {
    if (ManagerExpressionEvaluator.matchesVariablePattern(environmentValue)) {
      try {
        authHandler.authorizeAccountPermission(
            asList(new PermissionAttribute(PermissionType.ACCOUNT_MANAGEMENT, Action.READ)));
      } catch (WingsException ex) {
        throw new TriggerException(
            "User not authorized: Only members of the Account Administrator user group can create or update Triggers with parameterized variables.",
            USER);
      }
    } else {
      // Check if environment exist by envId
      Environment environment = environmentService.get(appId, environmentValue);
      if (environment != null) {
        try {
          authService.checkIfUserAllowedToDeployToEnv(appId, environmentValue);
        } catch (WingsException ex) {
          throw new TriggerException(
              "User does not have deployment execution permission on environment. [" + environment.getName() + "]",
              USER);
        }

      } else {
        // either environment does not exist or user give some random name.. then check account level permission
        try {
          authHandler.authorizeAccountPermission(
              asList(new PermissionAttribute(PermissionType.ACCOUNT_MANAGEMENT, Action.READ)));
        } catch (WingsException ex) {
          throw new TriggerException(
              "User not authorized: Only members of the Account Administrator user group can create or update Triggers with parameterized variables",
              USER);
        }
      }
    }
  }

  void authorizeWorkflowOrPipeline(String appId, String workflowOrPipelineId, boolean existing) {
    if (isEmpty(workflowOrPipelineId)) {
      if (existing) {
        return;
      }
    }
    PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.DEPLOYMENT, Action.EXECUTE);
    List<PermissionAttribute> permissionAttributeList = asList(permissionAttribute);
    authHandler.authorize(permissionAttributeList, asList(appId), workflowOrPipelineId);
  }
}
