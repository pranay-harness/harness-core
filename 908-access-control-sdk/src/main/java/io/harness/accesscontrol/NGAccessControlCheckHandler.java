package io.harness.accesscontrol;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.scope.ResourceScope;

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
      if (parameter.isAnnotationPresent(AccountIdentifier.class)) {
        builder.accountIdentifier((String) argument);
      }
      if (parameter.isAnnotationPresent(OrgIdentifier.class)) {
        builder.orgIdentifier((String) argument);
      }
      if (parameter.isAnnotationPresent(ProjectIdentifier.class)) {
        builder.projectIdentifier((String) argument);
      }
      if (parameter.isAnnotationPresent(ResourceIdentifier.class)) {
        builder.identifier((String) argument);
      }
    }
    return builder.build();
  }

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    NGAccessControlCheck ngAccessControlCheck = methodInvocation.getMethod().getAnnotation(NGAccessControlCheck.class);
    NGAccess ngAccess = getScopeIdentifiers(methodInvocation);
    accessControlClient.checkForAccessOrThrow(PermissionCheckDTO.builder()
                                                  .resourceScope(ResourceScope.builder()
                                                                     .accountIdentifier(ngAccess.getAccountIdentifier())
                                                                     .orgIdentifier(ngAccess.getOrgIdentifier())
                                                                     .projectIdentifier(ngAccess.getProjectIdentifier())
                                                                     .build())
                                                  .resourceType(ngAccessControlCheck.resourceType())
                                                  .resourceIdentifier(ngAccess.getIdentifier())
                                                  .permission(ngAccessControlCheck.permission())
                                                  .build());
    return methodInvocation.proceed();
  }
}
