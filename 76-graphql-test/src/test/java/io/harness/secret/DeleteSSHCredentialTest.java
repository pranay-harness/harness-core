package io.harness.secret;

import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

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
public class DeleteSSHCredentialTest extends GraphQLTest {
  @Inject SSHCredentialHelper sshCredentialHelper;
  @Inject DeleteRequestHelper deleteRequestHelper;

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testDeletingEncryptedText() {
    String secretId = sshCredentialHelper.createSSHCredential("secretName");
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
}
