/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secret;

import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.GraphQLTest;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.rule.Owner;
import io.harness.testframework.graphql.QLTestObject;

import software.wings.graphql.schema.mutation.secrets.payload.QLDeleteSecretPayload.QLDeleteSecretPayloadKeys;

import com.google.inject.Inject;
import graphql.ExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
    */ deleteRequestHelper.getDeleteSecretInput(secretId, "ENCRYPTED_TEXT"));
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
*/ deleteRequestHelper.getDeleteSecretInput(secretId, "ENCRYPTED_TEXT"));
    final ExecutionResult result = qlResult(query, getAccountId());
    assertThat(result.getErrors().size()).isEqualTo(1);
    assertThat(result.getErrors().get(0).getMessage())
        .isEqualTo(String.format(
            "Exception while fetching data (/deleteSecret) : Invalid request: No secret exists with the id %s",
            secretId));
  }
}
