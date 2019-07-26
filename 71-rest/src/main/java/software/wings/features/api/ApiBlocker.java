package software.wings.features.api;

import static io.harness.exception.WingsException.USER;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.exception.InvalidRequestException;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import software.wings.beans.User;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

public class ApiBlocker implements MethodInterceptor {
  @Inject Injector injector;

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    String accountId;
    if ((accountId = getAccountIdFromRequestContext()) == null) {
      accountId = getAccountId(methodInvocation).orElseThrow(IllegalStateException::new);
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
