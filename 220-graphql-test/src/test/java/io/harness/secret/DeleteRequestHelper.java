/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
