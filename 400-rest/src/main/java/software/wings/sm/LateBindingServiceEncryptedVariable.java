/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm;

import static software.wings.sm.ContextElement.SERVICE_VARIABLE;

import io.harness.expression.LateBindingValue;
import io.harness.expression.SecretString;
import io.harness.ff.FeatureFlagService;

import software.wings.beans.ServiceVariable;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.Map;
import lombok.Builder;

@Builder
class LateBindingServiceEncryptedVariable implements LateBindingValue {
  private ServiceVariable serviceVariable;
  private ExecutionContextImpl executionContext;
  private boolean adoptDelegateDecryption;
  private FeatureFlagService featureFlagService;
  private int expressionFunctorToken;
  private ManagerDecryptionService managerDecryptionService;
  private SecretManager secretManager;

  @Override
  public Object bind() {
    if (adoptDelegateDecryption) {
      return "${secretManager.obtain(\"" + serviceVariable.getSecretTextName() + "\", " + expressionFunctorToken + ")}";
    }
    managerDecryptionService.decrypt(serviceVariable,
        secretManager.getEncryptionDetails(
            serviceVariable, executionContext.getAppId(), executionContext.getWorkflowExecutionId()));
    SecretString value = SecretString.builder().value(new String(serviceVariable.getValue())).build();

    // Cache the secret as service variable if they are available.
    if (executionContext.getContextMap().containsKey(SERVICE_VARIABLE)) {
      ((Map<String, Object>) executionContext.getContextMap().get(SERVICE_VARIABLE))
          .put(serviceVariable.getName(), value);
    }
    return value;
  }
}
