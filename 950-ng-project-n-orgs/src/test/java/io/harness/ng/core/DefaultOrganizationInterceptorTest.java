/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core;

import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.KARAN;

import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.InjectorRuleMixin;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.google.inject.Module;
import java.lang.annotation.Annotation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

@OwnedBy(PL)
@Slf4j
public class DefaultOrganizationInterceptorTest extends CategoryTest {
  public static class DefaultOrgRule implements InjectorRuleMixin, MethodRule {
    @Override
    public List<Module> modules(List<Annotation> annotations) throws Exception {
      return singletonList(new DefaultOrganizationModule());
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
      return applyInjector(log, base, method, target);
    }
  }
  @Rule public DefaultOrgRule defaultOrgRule = new DefaultOrgRule();

  public static class Methods {
    @DefaultOrganization
    public Pair<String, String> methodOne(
        @OrgIdentifier String orgIdentifier, @ProjectIdentifier String projectIdentifier) {
      return Pair.of(orgIdentifier, projectIdentifier);
    }

    @DefaultOrganization
    public Pair<String, String> methodTwo(@OrgIdentifier String orgIdentifier, String projectIdentifier) {
      return Pair.of(orgIdentifier, projectIdentifier);
    }
  }

  @Inject private Methods methods;

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testNullOrgIdentifier() {
    Pair<String, String> methodOneResult = methods.methodOne(null, PROJECT_KEY);
    assertEquals(DEFAULT_ORG_IDENTIFIER, methodOneResult.getLeft());
    assertEquals(PROJECT_KEY, methodOneResult.getRight());

    methodOneResult = methods.methodOne(null, null);
    assertNull(methodOneResult.getLeft());
    assertNull(methodOneResult.getRight());

    Pair<String, String> methodTwoResult = methods.methodTwo(null, PROJECT_KEY);
    assertNull(methodTwoResult.getLeft());
    assertEquals(PROJECT_KEY, methodTwoResult.getRight());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testNonNullOrgIdentifier() {
    Pair<String, String> methodOneResult = methods.methodOne(ORG_KEY, PROJECT_KEY);
    assertEquals(ORG_KEY, methodOneResult.getLeft());
    assertEquals(PROJECT_KEY, methodOneResult.getRight());

    methodOneResult = methods.methodOne(ORG_KEY, null);
    assertEquals(ORG_KEY, methodOneResult.getLeft());
    assertNull(methodOneResult.getRight());

    Pair<String, String> methodTwoResult = methods.methodTwo(ORG_KEY, PROJECT_KEY);
    assertEquals(ORG_KEY, methodTwoResult.getLeft());
    assertEquals(PROJECT_KEY, methodTwoResult.getRight());
  }
}
