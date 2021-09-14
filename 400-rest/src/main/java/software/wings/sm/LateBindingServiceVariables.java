/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.MASKED;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.sm.ContextElement.SAFE_DISPLAY_SERVICE_VARIABLE;
import static software.wings.sm.ContextElement.SERVICE_VARIABLE;

import io.harness.expression.LateBindingValue;

import software.wings.beans.NameValuePair;
import software.wings.beans.ServiceVariable;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;

@Builder
class LateBindingServiceVariables implements LateBindingValue {
  private ServiceVariableService.EncryptedFieldMode encryptedFieldMode;
  private List<NameValuePair> phaseOverrides;

  private ExecutionContextImpl executionContext;
  private ManagerDecryptionService managerDecryptionService;
  private SecretManager secretManager;
  private boolean adoptDelegateDecryption;
  private int expressionFunctorToken;

  @Override
  public Object bind() {
    String key = encryptedFieldMode == OBTAIN_VALUE ? SERVICE_VARIABLE : SAFE_DISPLAY_SERVICE_VARIABLE;
    executionContext.getContextMap().remove(key);

    Map<String, Object> variables = isEmpty(phaseOverrides)
        ? new HashMap<>()
        : phaseOverrides.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));

    List<ServiceVariable> serviceVariables = executionContext.prepareServiceVariables(encryptedFieldMode == MASKED
            ? ServiceTemplateService.EncryptedFieldComputeMode.MASKED
            : ServiceTemplateService.EncryptedFieldComputeMode.OBTAIN_META);

    if (isNotEmpty(serviceVariables)) {
      serviceVariables.forEach(serviceVariable -> {
        executionContext.prepareVariables(
            encryptedFieldMode, serviceVariable, variables, adoptDelegateDecryption, expressionFunctorToken);
      });
    }
    executionContext.getContextMap().put(key, variables);
    return variables;
  }
}
