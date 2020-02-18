package io.harness.secret;

import io.harness.GraphQLTest;

public class DeleteRequestHelper extends GraphQLTest {
  public String getDeleteSecretInput(String secretId) {
    return $GQL(/* {
        clientMutationId: "abc",
        secretId: "%s"
    }
    */ secretId);
  }
}
