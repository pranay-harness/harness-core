/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core;

import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

@OwnedBy(PL)
@Singleton
public class DefaultOrganizationInterceptor implements MethodInterceptor {
  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    Method method = methodInvocation.getMethod();
    String projectIdentifier = null;
    String orgIdentifier = null;
    int orgIdentifierIndex = -1;
    for (int i = 0; i < method.getParameters().length; i++) {
      Parameter parameter = method.getParameters()[i];
      if (parameter.isAnnotationPresent(OrgIdentifier.class)) {
        if (methodInvocation.getArguments()[i] != null) {
          orgIdentifier = methodInvocation.getArguments()[i].toString();
        }
        orgIdentifierIndex = i;
      }
      if (parameter.isAnnotationPresent(ProjectIdentifier.class) && methodInvocation.getArguments()[i] != null) {
        projectIdentifier = methodInvocation.getArguments()[i].toString();
      }
    }
    if (projectIdentifier != null && orgIdentifier == null && orgIdentifierIndex != -1) {
      methodInvocation.getArguments()[orgIdentifierIndex] = DEFAULT_ORG_IDENTIFIER;
    }
    return methodInvocation.proceed();
  }
}
