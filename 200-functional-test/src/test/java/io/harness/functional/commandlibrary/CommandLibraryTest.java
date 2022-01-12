/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.commandlibrary;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CommandLibraryTest extends AbstractFunctionalTest {
  @Test
  @Owner(developers = ROHIT_KUMAR, intermittent = true)
  @Category(FunctionalTests.class)
  public void test_getStores() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    final String authToken = Setup.getAuthToken(READ_ONLY_USER, "readonlyuser");
    final Response response = Setup.portal()
                                  .auth()
                                  .oauth2(authToken)
                                  .queryParam("accountId", getAccount().getUuid())
                                  .get("/command-library-service/command-stores");
    assertThat(response.getStatusCode() == HttpStatus.SC_OK).isTrue();
    assertThat(response.body().jsonPath().getString("resource[0].name")).isEqualTo("harness");
  }

  @Override
  protected boolean needCommandLibraryService() {
    return true;
  }
}
