package io.harness.delegate.exceptionhandler.handler;

import static io.harness.eraro.ErrorCode.VAULT_OPERATION_ERROR;
import static io.harness.exception.NestedExceptionUtils.hintWithExplanationException;
import static io.harness.exception.WingsException.USER;

import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
import io.harness.exception.runtime.HashiCorpVaultRuntimeException;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class HashicorpVaultExceptionHandler implements ExceptionHandler {
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(HashiCorpVaultRuntimeException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    HashiCorpVaultRuntimeException ex = (HashiCorpVaultRuntimeException) exception;
    return hintWithExplanationException(HintException.HINT_HASHICORP_VAULT_SM_ACCESS_DENIED,
        ExplanationException.INVALID_PARAMETER,
        new InvalidRequestException(ex.getMessage(), VAULT_OPERATION_ERROR, USER));
  }
}