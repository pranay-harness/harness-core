package io.harness.secret;

import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import graphql.ExecutionResult;
import io.harness.GraphQLTest;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.rule.Owner;
import io.harness.testframework.graphql.QLTestObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.graphql.schema.mutation.secrets.payload.QLDeleteSecretPayload.QLDeleteSecretPayloadKeys;

@Slf4j
public class DeleteEncryptedTextTest extends GraphQLTest {
  @Inject EncryptedTextHelper encryptedTextHelper;
  @Inject DeleteRequestHelper deleteRequestHelper;

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testDeletingEncryptedText() {
    String secretId = encryptedTextHelper.CreateEncryptedText("secretName");
    String query = $GQL(/*
    mutation{
        deleteSecret(input:%s){
            clientMutationId
        }
    }
    */ deleteRequestHelper.getDeleteSecretInput(secretId));
    final QLTestObject qlTestObject = qlExecute(query, getAccountId());
    assertThat(qlTestObject.get(QLDeleteSecretPayloadKeys.clientMutationId)).isEqualTo("abc");
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testDeletingSecretWithInvalidId() {
    String secretId = "invalidSecretId";
    String query = $GQL(/*
mutation{
    deleteSecret(input:%s){
        clientMutationId
    }
}
*/ deleteRequestHelper.getDeleteSecretInput(secretId));
    final ExecutionResult result = qlResult(query, getAccountId());
    assertThat(result.getErrors().size()).isEqualTo(1);
    assertThat(result.getErrors().get(0).getMessage())
        .isEqualTo(String.format(
            "Exception while fetching data (/deleteSecret) : Invalid request: No secret exists with the id %s",
            secretId));
  }
}
