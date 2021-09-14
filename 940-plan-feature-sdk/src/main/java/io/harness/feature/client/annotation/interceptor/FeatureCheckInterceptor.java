package io.harness.feature.client.annotation.interceptor;

import static io.harness.exception.WingsException.USER_SRE;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.exception.InvalidArgumentsException;
import io.harness.feature.client.annotation.FeatureRestrictionCheck;
import io.harness.feature.client.services.PlanFeatureClientService;
import io.harness.feature.constants.FeatureRestriction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Parameter;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

@Slf4j
@Singleton
public class FeatureCheckInterceptor implements MethodInterceptor {
  @Inject PlanFeatureClientService planFeatureClientService;

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    FeatureRestrictionCheck featureCheck =
        methodInvocation.getMethod().getDeclaredAnnotation(FeatureRestrictionCheck.class);

    Optional<String> accountIdentifierOptional = getAccountIdentifier(methodInvocation);
    if (!accountIdentifierOptional.isPresent()) {
      throw new InvalidArgumentsException("Account id is not marked in the request", USER_SRE);
    }

    String accountIdentifier = accountIdentifierOptional.get();
    FeatureRestriction featureName = featureCheck.value();

    planFeatureClientService.checkAvailability(featureName, accountIdentifier);
    return methodInvocation.proceed();
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
