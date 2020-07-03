package software.wings.service.impl.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.ACCESS_DENIED;
import static io.harness.exception.WingsException.USER;
import static java.util.Arrays.asList;
import static software.wings.security.AuthRuleFilter.getAllowedAppIds;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.TriggerException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import software.wings.beans.Environment;
import software.wings.beans.User;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.impl.security.auth.DeploymentAuthHandler;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;

import java.util.List;
import java.util.Set;

@OwnedBy(CDC)
@Singleton
public class TriggerAuthHandler {
  @Inject private AuthHandler authHandler;
  @Inject private DeploymentAuthHandler deploymentAuthHandler;
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
    if (isEmpty(workflowOrPipelineId) && existing) {
      return;
    }
    deploymentAuthHandler.authorizeWorkflowOrPipelineForExecution(appId, workflowOrPipelineId);
  }

  public void authorizeAppAccess(List<String> appIds, String accountId) {
    User user = UserThreadLocal.get();
    UserPermissionInfo userPermissionInfo = authService.getUserPermissionInfo(accountId, user, false);
    Set<String> allowedAppIds = getAllowedAppIds(userPermissionInfo);
    if (!allowedAppIds.containsAll(appIds)) {
      throw new UnauthorizedException("User Not authorized", ACCESS_DENIED, USER);
    }
  }
}
