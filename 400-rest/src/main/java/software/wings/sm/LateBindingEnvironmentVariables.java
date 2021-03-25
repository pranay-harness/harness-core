package software.wings.sm;

import static io.harness.data.structure.HasPredicate.hasSome;

import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.sm.ContextElement.ENVIRONMENT_VARIABLE;

import io.harness.expression.LateBindingValue;

import software.wings.beans.Environment;
import software.wings.beans.ServiceVariable;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
class LateBindingEnvironmentVariables implements LateBindingValue {
  private ExecutionContextImpl executionContext;
  private ServiceVariableService serviceVariableService;
  private ManagerDecryptionService managerDecryptionService;
  private SecretManager secretManager;
  private boolean adoptDelegateDecryption;
  private int expressionFunctorToken;

  @Override
  public Object bind() {
    Map<String, Object> variables = new HashMap<>();

    Environment environment = executionContext.getEnv();

    if (environment == null) {
      executionContext.getContextMap().put(ENVIRONMENT_VARIABLE, variables);
      return variables;
    }

    executionContext.getContextMap().remove(ENVIRONMENT_VARIABLE);

    List<ServiceVariable> serviceVariables = serviceVariableService.getServiceVariablesForEntity(
        executionContext.getAppId(), environment.getUuid(), OBTAIN_VALUE);

    executionContext.prepareServiceVariables(ServiceTemplateService.EncryptedFieldComputeMode.OBTAIN_META);

    if (hasSome(serviceVariables)) {
      serviceVariables.forEach(serviceVariable -> {
        executionContext.prepareVariables(ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE, serviceVariable,
            variables, adoptDelegateDecryption, expressionFunctorToken);
      });
    }
    executionContext.getContextMap().put(ENVIRONMENT_VARIABLE, variables);
    return variables;
  }
}
