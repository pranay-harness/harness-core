package io.harness.delegate.exceptionhandler.handler;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.AZURE_KEY_VAULT_OPERATION_ERROR;
import static io.harness.exception.NestedExceptionUtils.hintWithExplanationException;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.keyvault.models.KeyVaultErrorException;
import com.microsoft.rest.RestException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PL)
@Singleton
public class AzureVaultSecretManagerExceptionHandler implements ExceptionHandler {
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder()
        .add(RestException.class)
        .build(); // RestException is core exception from com.microsoft.rest package
  }

  @Override
  public WingsException handleException(Exception exception) {
    RestException ex = (RestException) exception;
    if (ex instanceof KeyVaultErrorException) {
      return hintWithExplanationException(HintException.HINT_AZURE_VAULT_SM_CRUD_DENIED,
          ExplanationException.AZURE_SM_VAULT_ENGINE_PERMISSION,
          new InvalidRequestException(ex.getMessage(), AZURE_KEY_VAULT_OPERATION_ERROR, USER));
    } else if (ex instanceof CloudException) {
      return hintWithExplanationException(HintException.HINT_AZURE_VAULT_SM_SUBSCRIPTION_ID_ERROR,
          ExplanationException.AZURE_SM_VAULT_ENGINE_FETCH_ERROR_SUBSCRIPTION_ID,
          new InvalidRequestException(ex.getMessage(), AZURE_KEY_VAULT_OPERATION_ERROR, USER));
    } else {
      return hintWithExplanationException(HintException.HINT_AZURE_VAULT_SM_ACCESS_DENIED,
          ExplanationException.INVALID_PARAMETER,
          new InvalidRequestException(ex.getMessage(), AZURE_KEY_VAULT_OPERATION_ERROR, USER));
    }
  }
}