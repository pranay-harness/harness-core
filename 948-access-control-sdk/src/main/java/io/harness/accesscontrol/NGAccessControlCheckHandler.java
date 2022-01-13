/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
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
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier()),
        Resource.of(ngAccessControlCheck.resourceType(), ngAccess.getIdentifier()), ngAccessControlCheck.permission());
    return methodInvocation.proceed();
  }
}
