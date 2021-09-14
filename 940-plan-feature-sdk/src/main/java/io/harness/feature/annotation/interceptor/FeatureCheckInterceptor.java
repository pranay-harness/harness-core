/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.annotation.interceptor;

import static io.harness.exception.WingsException.USER_SRE;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.exception.InvalidArgumentsException;
import io.harness.feature.annotation.FeatureCheck;
import io.harness.feature.services.FeatureService;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

@Slf4j
@Singleton
public class FeatureCheckInterceptor implements MethodInterceptor {
  @Inject FeatureService featureService;
  @Inject PersistentLocker persistentLocker;

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    FeatureCheck featureCheck = methodInvocation.getMethod().getDeclaredAnnotation(FeatureCheck.class);

    Optional<String> accountIdentifierOptional = getAccountIdentifier(methodInvocation);
    if (!accountIdentifierOptional.isPresent()) {
      throw new InvalidArgumentsException("Account id is not marked in the request", USER_SRE);
    }

    String accountIdentifier = accountIdentifierOptional.get();
    String featureName = featureCheck.value();

    AcquiredLock lock = null;
    if (featureService.isLockRequired(featureName, accountIdentifier)) {
      lock = persistentLocker.waitToAcquireLock(
          accountIdentifier + featureName, Duration.ofSeconds(20), Duration.ofSeconds(30));
    }

    try {
      featureService.checkAvailabilityOrThrow(featureName, accountIdentifier);
      return methodInvocation.proceed();
    } finally {
      if (lock != null) {
        lock.close();
      }
    }
  }

  private Optional<String> getAccountIdentifier(MethodInvocation methodInvocation) {
    Parameter[] parameters = methodInvocation.getMethod().getParameters();
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      if (parameter.isAnnotationPresent(AccountIdentifier.class)) {
        return Optional.of(String.valueOf(methodInvocation.getArguments()[i]));
      }
    }
    return Optional.empty();
  }
}
