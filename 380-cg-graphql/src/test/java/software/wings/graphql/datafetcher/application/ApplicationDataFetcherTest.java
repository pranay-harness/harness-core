/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.application;

import static io.harness.rule.OwnerRule.PRABU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.within;

import io.harness.category.element.UnitTests;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.Application;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.query.QLApplicationQueryParameters;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.security.AppPermissionSummaryForUI;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AppService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApplicationDataFetcherTest extends AbstractDataFetcherTestBase {
  public static final String APPLICATION_NAME = "APPLICATION_NAME";
  @Inject ApplicationDataFetcher applicationDataFetcher;
  @Inject AppService appService;
  private User user;

  @Before
  public void setup() throws SQLException {
    user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testApplicationDataFetcher() {
    Application app = createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APPLICATION_NAME, TAG_TEAM, TAG_VALUE_TEAM1);
    app.setDescription("DESC");
    appService.save(app);
    user.setUserRequestContext(
        UserRequestContext.builder().userPermissionInfo(UserPermissionInfo.builder().build()).build());
    user.getUserRequestContext().getUserPermissionInfo().setAppPermissionMap(
        ImmutableMap.of(APP1_ID_ACCOUNT1, AppPermissionSummaryForUI.builder().build()));
    QLApplication qlApplication = applicationDataFetcher.fetch(
        QLApplicationQueryParameters.builder().applicationId(APP1_ID_ACCOUNT1).build(), ACCOUNT1_ID);
    assertThat(qlApplication.getId()).isEqualTo(APP1_ID_ACCOUNT1);
    assertThat(qlApplication.getName()).isEqualTo(APPLICATION_NAME);

    qlApplication = applicationDataFetcher.fetch(
        QLApplicationQueryParameters.builder().name(APPLICATION_NAME).build(), ACCOUNT1_ID);
    assertThat(qlApplication.getId()).isEqualTo(APP1_ID_ACCOUNT1);
    assertThat(qlApplication.getName()).isEqualTo(APPLICATION_NAME);
    assertThat(qlApplication.getDescription()).isEqualTo("DESC");
    assertThat(qlApplication.getCreatedAt()).isCloseTo(System.currentTimeMillis(), within(60000L));

    assertThatThrownBy(()
                           -> applicationDataFetcher.fetch(
                               QLApplicationQueryParameters.builder().name(APPLICATION_NAME + 2).build(), ACCOUNT1_ID))
        .isInstanceOf(InvalidRequestException.class);

    try {
      applicationDataFetcher.fetch(QLApplicationQueryParameters.builder().name(APPLICATION_NAME).build(), ACCOUNT2_ID);
      fail("InvalidRequestException expected here");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(InvalidRequestException.class);
    }
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldThrowExceptionForInaccessibleApp() {
    Application app = createApp(ACCOUNT1_ID, APP2_ID_ACCOUNT1, APPLICATION_NAME, TAG_TEAM, TAG_VALUE_TEAM1);
    app.setDescription("DESC");
    appService.save(app);
    user.setUserRequestContext(
        UserRequestContext.builder().userPermissionInfo(UserPermissionInfo.builder().build()).build());
    user.getUserRequestContext().getUserPermissionInfo().setAppPermissionMap(
        ImmutableMap.of(APP1_ID_ACCOUNT1, AppPermissionSummaryForUI.builder().build()));
    assertThatThrownBy(
        ()
            -> applicationDataFetcher.fetch(
                QLApplicationQueryParameters.builder().applicationId(APP2_ID_ACCOUNT1).build(), ACCOUNT1_ID))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Not authorized to access the app");
  }
}
