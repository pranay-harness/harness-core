/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features.api;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.User;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

@OwnedBy(PL)
@Singleton
@TargetModule(HarnessModule._970_RBAC_CORE)
public class ApiBlocker implements MethodInterceptor {
  @Inject Injector injector;

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    String accountId;
    if ((accountId = getAccountIdFromRequestContext()) == null) {
      accountId =
          getAccountId(methodInvocation)
              .orElseThrow(
                  ()
                      -> new IllegalStateException(
                          "Could not get accountId from method arguments. Use software.wings.features.api.@AccountId , or software.wings.features.api.@GetAccountId annotation on method arguments."));
    }

    if (accountId.equals(GLOBAL_ACCOUNT_ID) || getFeature(methodInvocation).isAvailableForAccount(accountId)) {
      return methodInvocation.proceed();
    }

    throw new InvalidRequestException(String.format("Operation not permitted for account [%s].", accountId), USER);
  }

  private String getAccountIdFromRequestContext() {
    User user = UserThreadLocal.get();
    if (user == null) {
      return null;
    }

    UserRequestContext userRequestContext = user.getUserRequestContext();
    if (user.getUserRequestContext() == null) {
      return null;
    }

    return userRequestContext.getAccountId();
  }

  @SuppressWarnings("unchecked")
  private Optional<String> getAccountId(MethodInvocation methodInvocation)
      throws IllegalAccessException, InstantiationException {
    Method method = methodInvocation.getMethod();
    String accountId = null;
    for (int i = 0; i < method.getParameters().length; i++) {
      Parameter param = method.getParameters()[i];
      Object argument = methodInvocation.getArguments()[i];
      if (param.isAnnotationPresent(GetAccountId.class)) {
        AccountIdExtractor accountIdExtractor = param.getAnnotation(GetAccountId.class).value().newInstance();
        accountId = accountIdExtractor.getAccountId(argument);
        break;
      }
      if (param.isAnnotationPresent(AccountId.class)) {
        accountId = (String) argument;
        break;
      }
    }

    return Optional.ofNullable(accountId);
  }

  private PremiumFeature getFeature(MethodInvocation methodInvocation) {
    Class<? extends PremiumFeature> restrictedFeatureClazz =
        methodInvocation.getMethod().getAnnotation(RestrictedApi.class).value();

    return injector.getInstance(restrictedFeatureClazz);
  }
}
