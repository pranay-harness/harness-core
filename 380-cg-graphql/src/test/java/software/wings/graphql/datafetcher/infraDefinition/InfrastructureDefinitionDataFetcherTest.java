/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.infraDefinition;

import static io.harness.rule.OwnerRule.PARDHA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.query.QLInfrastructureDefinitionQueryParameters;
import software.wings.graphql.schema.type.QLInfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition;
import software.wings.security.AppPermissionSummaryForUI;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InfrastructureDefinitionDataFetcherTest extends AbstractDataFetcherTestBase {
  @Inject InfrastructureDefinitionDataFetcher infrastructureDefinitionDataFetcher;
  private User user;

  @Before
  public void setup() throws SQLException {
    user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);
    createEnv(ACCOUNT1_ID, APP1_ID_ACCOUNT1, ENV1_ID_APP1_ACCOUNT1, ENV1_ID_APP1_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);
  }

  @Test
  @Owner(developers = PARDHA)
  @Category(UnitTests.class)
  public void testInfrastructureDefinitionDataFetcher() {
    user.setUserRequestContext(
        UserRequestContext.builder().userPermissionInfo(UserPermissionInfo.builder().build()).build());
    user.getUserRequestContext().getUserPermissionInfo().setAppPermissionMap(
        ImmutableMap.of(APP1_ID_ACCOUNT1, AppPermissionSummaryForUI.builder().build()));
    InfrastructureDefinition infrastructureDefinition = createInfrastructureDefinition(ACCOUNT1_ID,
        ENV1_ID_APP1_ACCOUNT1, APP1_ID_ACCOUNT1, INFRA1_ID_ENV1_APP1_ACCOUNT1, INFRA1_ID_ENV1_APP1_ACCOUNT1);
    QLInfrastructureDefinition qlInfrastructureDefinition = infrastructureDefinitionDataFetcher.fetch(
        QLInfrastructureDefinitionQueryParameters.builder().infrastructureId(INFRA1_ID_ENV1_APP1_ACCOUNT1).build(),
        ACCOUNT1_ID);
    assertThat(qlInfrastructureDefinition.getId()).isEqualTo(INFRA1_ID_ENV1_APP1_ACCOUNT1);
    assertThat(qlInfrastructureDefinition.getName()).isEqualTo(INFRA1_ID_ENV1_APP1_ACCOUNT1);

    try {
      qlInfrastructureDefinition = infrastructureDefinitionDataFetcher.fetch(
          QLInfrastructureDefinitionQueryParameters.builder().infrastructureId(INFRA1_ID_ENV1_APP1_ACCOUNT1).build(),
          ACCOUNT2_ID);
    } catch (InvalidRequestException e) {
      assertThat(e.getMessage()).isEqualTo("Infrastructure does not exist");
    }
  }

  @Test
  @Owner(developers = PARDHA)
  @Category(UnitTests.class)
  public void testInfrastructureDefinitionDataFetcherWithIncorrectInfraId() {
    user.setUserRequestContext(
        UserRequestContext.builder().userPermissionInfo(UserPermissionInfo.builder().build()).build());
    user.getUserRequestContext().getUserPermissionInfo().setAppPermissionMap(
        ImmutableMap.of(APP1_ID_ACCOUNT1, AppPermissionSummaryForUI.builder().build()));
    InfrastructureDefinition infrastructureDefinition = createInfrastructureDefinition(ACCOUNT1_ID,
        ENV1_ID_APP1_ACCOUNT1, APP1_ID_ACCOUNT1, INFRA1_ID_ENV1_APP1_ACCOUNT1, INFRA1_ID_ENV1_APP1_ACCOUNT1);
    QLInfrastructureDefinition qlInfrastructureDefinition = infrastructureDefinitionDataFetcher.fetch(
        QLInfrastructureDefinitionQueryParameters.builder().infrastructureId(INFRA1_ID_ENV1_APP1_ACCOUNT1).build(),
        ACCOUNT1_ID);
    assertThat(qlInfrastructureDefinition.getId()).isEqualTo(INFRA1_ID_ENV1_APP1_ACCOUNT1);
    assertThat(qlInfrastructureDefinition.getName()).isEqualTo(INFRA1_ID_ENV1_APP1_ACCOUNT1);

    try {
      qlInfrastructureDefinition = infrastructureDefinitionDataFetcher.fetch(
          QLInfrastructureDefinitionQueryParameters.builder().infrastructureId(INFRA2_ID_ENV1_APP1_ACCOUNT1).build(),
          ACCOUNT1_ID);
    } catch (InvalidRequestException ex) {
      assertThat(ex.getMessage()).isEqualTo("Infrastructure does not exist");
    }
  }
}
