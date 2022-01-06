/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.service;

import static io.harness.rule.OwnerRule.RUSHABH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.Service;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.query.QLServiceQueryParameters;
import software.wings.graphql.schema.type.QLService;
import software.wings.security.UserThreadLocal;

import com.google.inject.Inject;
import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServiceDataFetcherTest extends AbstractDataFetcherTestBase {
  @Inject ServiceDataFetcher serviceDataFetcher;
  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testServiceDataFetcher() {
    Service service = createService(ACCOUNT1_ID, APP1_ID_ACCOUNT1, SERVICE1_ID_APP1_ACCOUNT1, SERVICE1_ID_APP1_ACCOUNT1,
        TAG_MODULE, TAG_VALUE_MODULE1);
    QLService qlService = serviceDataFetcher.fetch(
        QLServiceQueryParameters.builder().serviceId(SERVICE1_ID_APP1_ACCOUNT1).build(), ACCOUNT1_ID);
    assertThat(qlService.getId()).isEqualTo(SERVICE1_ID_APP1_ACCOUNT1);
    assertThat(qlService.getName()).isEqualTo(SERVICE1_ID_APP1_ACCOUNT1);

    qlService = serviceDataFetcher.fetch(
        QLServiceQueryParameters.builder().serviceId(SERVICE2_ID_APP1_ACCOUNT1).build(), ACCOUNT1_ID);
    assertThat(qlService).isNull();

    try {
      qlService = serviceDataFetcher.fetch(
          QLServiceQueryParameters.builder().serviceId(SERVICE1_ID_APP1_ACCOUNT1).build(), ACCOUNT2_ID);

      fail("InvalidRequestException expected here");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(InvalidRequestException.class);
    }
  }
}
