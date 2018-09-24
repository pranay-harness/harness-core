package software.wings.licensing;

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.annotation.Annotation;

/**
 * Created by peeyushaggarwal on 3/22/17.
 */
public class LicenseInterceptor implements MethodInterceptor {
  @Inject private LicenseManager licenseManager;

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    String licenseKey = extractLicenseKey(invocation.getMethod().getParameterAnnotations(), invocation.getArguments());
    String operation = invocation.getMethod().getName();
    if (isNotBlank(licenseKey)) {
      licenseManager.validateLicense(licenseKey, operation);
    }
    return invocation.proceed();
  }

  private String extractLicenseKey(Annotation[][] annotations, Object[] arguments) {
    for (int i = 0; i < annotations.length; i++) {
      Annotation[] parameterAnnotations = annotations[i];
      if (stream(parameterAnnotations)
              .filter(annotation -> annotation.annotationType().isAssignableFrom(LicenseKey.class))
              .findFirst()
              .isPresent()) {
        return (String) arguments[i];
      }
    }
    return null;
  }
}
