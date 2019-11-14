package io.harness;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import graphql.ExecutionResult;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.data.structure.EmptyPredicate;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;
import java.util.Map;

public class GraphQLExceptionHandlingTest extends GraphQLTest {
  private static final String INCORRECT_ENVIRONMENT_ID = "TEST";

  @Test
  @Owner(emails = UNKNOWN)
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

    final ExecutionResult result = qlResult(query, "dummy_account");
    Map<String, Object> spec = result.toSpecification();
    List error = (List) spec.get("errors");
    if (EmptyPredicate.isNotEmpty(error)) {
      Map errorMsgMap = (Map) error.get(0);
      String message = (String) errorMsgMap.get("message");
      assertThat(message).contains(INCORRECT_ENVIRONMENT_ID + " is not found");
    }
  }
}
