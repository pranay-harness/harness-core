/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.util;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.util.GithubApiFunctorTestHelper.RESOLVED_EXPRESSION_REGEX;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.rule.Owner;
import io.harness.stateutils.buildstate.ConnectorUtils;

import java.util.Map;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@OwnedBy(HarnessTeam.CI)
@RunWith(MockitoJUnitRunner.class)
public class GithubApiTokenEvaluatorTest extends CategoryTest {
  @Mock ConnectorUtils connectorUtils;

  private static NGAccess ngAccess;
  private static Pattern pattern;

  private GithubApiFunctorTestHelper.TestObject testObject;

  @BeforeClass
  public static void beforeClass() throws Exception {
    ngAccess = BaseNGAccess.builder()
                   .accountIdentifier("AccountId")
                   .orgIdentifier("OrgIdentifier")
                   .projectIdentifier("ProjectIdentifier")
                   .build();
    pattern = Pattern.compile(RESOLVED_EXPRESSION_REGEX);
  }

  @Before
  public void setUp() throws Exception {
    testObject = GithubApiFunctorTestHelper.getTestObject();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testFunctorEvaluation() {
    GithubApiTokenEvaluator githubApiTokenEvaluator =
        GithubApiTokenEvaluator.builder()
            .githubApiFunctorConfig(GithubApiFunctor.Config.builder()
                                        .codeBaseConnectorRef("codeBaseConnectorRef")
                                        .fetchConnector(false)
                                        .build())
            .build();

    githubApiTokenEvaluator.resolve(testObject, ngAccess, 1);

    assertThat(testObject.getExpression().fetchFinalValue()).asString().matches(pattern);
    assertThat(testObject.getExpressionAsString()).matches(pattern);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testConnectorFetching() {
    GithubApiTokenEvaluator githubApiTokenEvaluator =
        GithubApiTokenEvaluator.builder()
            .githubApiFunctorConfig(GithubApiFunctor.Config.builder()
                                        .codeBaseConnectorRef("codeBaseConnectorRef")
                                        .fetchConnector(true)
                                        .build())
            .connectorUtils(connectorUtils)
            .build();

    when(connectorUtils.getConnectorDetails(eq(ngAccess), eq("codeBaseConnectorRef")))
        .thenReturn(ConnectorDetails.builder().build());

    Map<String, ConnectorDetails> connectorDetailsMap = githubApiTokenEvaluator.resolve(testObject, ngAccess, 1);

    assertThat(connectorDetailsMap).hasSize(1);
    connectorDetailsMap.keySet().forEach(
        s -> assertThat(testObject.getExpression().fetchFinalValue()).asString().contains(s));
  }
}
