/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.rule.OwnerRule.VIKAS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.data.structure.EmptyPredicate;
import io.harness.rule.Owner;

import graphql.ExecutionResult;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GraphQLExceptionHandlingTest extends GraphQLTest {
  private static final String INCORRECT_ENVIRONMENT_ID = "TEST";

  @Test
  @Owner(developers = VIKAS)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryEnvironment() {
    String query = $GQL(/*
{
  environment(environmentId: "%s") {
    id
    name
    description
    type
    createdAt
    createdBy {
      id
    }
  }
}*/ INCORRECT_ENVIRONMENT_ID);

    final ExecutionResult result = qlResult(query, getAccountId());
    Map<String, Object> spec = result.toSpecification();
    List error = (List) spec.get("errors");
    if (EmptyPredicate.isNotEmpty(error)) {
      Map errorMsgMap = (Map) error.get(0);
      String message = (String) errorMsgMap.get("message");
      assertThat(message).contains(INCORRECT_ENVIRONMENT_ID + " is not found");
    }
  }
}
