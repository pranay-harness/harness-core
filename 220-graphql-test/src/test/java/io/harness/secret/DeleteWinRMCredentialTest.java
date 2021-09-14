/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class DeleteWinRMCredentialTest extends GraphQLTest {
  @Inject WinRMCredentialHelper winRMCredentialHelper;
  @Inject DeleteRequestHelper deleteRequestHelper;

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testDeletingEncryptedText() {
    String secretId = winRMCredentialHelper.createWinRMCredential("secretName");
    String query = $GQL(/*
    mutation{
        deleteSecret(input:%s){
            clientMutationId
        }
    }
    */ deleteRequestHelper.getDeleteSecretInput(secretId, "WINRM_CREDENTIAL"));
    final QLTestObject qlTestObject = qlExecute(query, getAccountId());
    assertThat(qlTestObject.get(QLDeleteSecretPayloadKeys.clientMutationId)).isEqualTo("abc");
  }
}
