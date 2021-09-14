/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.secret;

import io.harness.GraphQLTest;

public class DeleteRequestHelper extends GraphQLTest {
  public String getDeleteSecretInput(String secretId, String secretType) {
    return $GQL(/* {
        clientMutationId: "abc",
        secretId: "%s",
        secretType: %s
    }
    */ secretId, secretType);
  }
}
