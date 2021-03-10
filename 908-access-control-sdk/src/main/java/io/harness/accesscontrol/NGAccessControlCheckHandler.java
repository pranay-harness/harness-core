package io.harness.accesscontrol;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;

import com.google.inject.Inject;
import java.lang.reflect.Parameter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

@NoArgsConstructor
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.PL)
public class NGAccessControlCheckHandler implements MethodInterceptor {
  @Inject private AccessControlClient accessControlClient;

  NGAccess getScopeIdentifiers(MethodInvocation methodInvocation) {
    BaseNGAccess.Builder builder = BaseNGAccess.builder();
    Parameter[] parameters = methodInvocation.getMethod().getParameters();
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      Object argument = methodInvocation.getArguments()[i];
      if (parameter.isAnnotationPresent(AccountIdentifier.class)
          || parameter.getName().equals(NGCommonEntityConstants.ACCOUNT_KEY)) {
        builder.accountIdentifier((String) argument);
      }
      if (parameter.isAnnotationPresent(OrgIdentifier.class)
          || parameter.getName().equals(NGCommonEntityConstants.ORG_KEY)) {
        builder.orgIdentifier((String) argument);
      }
      if (parameter.isAnnotationPresent(ProjectIdentifier.class)
          || parameter.getName().equals(NGCommonEntityConstants.PROJECT_KEY)) {
        builder.projectIdentifier((String) argument);
      }
      if (parameter.isAnnotationPresent(ResourceIdentifier.class)
          || parameter.getName().equals(NGCommonEntityConstants.IDENTIFIER_KEY)) {
        builder.identifier((String) argument);
      }
    }
    return builder.build();
  }

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    NGAccessControlCheck ngAccessControlCheck = methodInvocation.getMethod().getAnnotation(NGAccessControlCheck.class);
    NGAccess ngAccess = getScopeIdentifiers(methodInvocation);
    accessControlClient.checkAccessOrThrow(ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(),
        ngAccess.getProjectIdentifier(), ngAccessControlCheck.resourceType(), ngAccess.getIdentifier(),
        ngAccessControlCheck.permissionIdentifier());
    return methodInvocation.proceed();
  }
}
