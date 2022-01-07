/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.connector;

import static io.harness.rule.OwnerRule.PRABU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.within;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.CustomArtifactServerConfig;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.application.ApplicationDataFetcher;
import software.wings.graphql.schema.query.QLConnectorQueryParameters;
import software.wings.graphql.schema.type.connector.QLConnector;
import software.wings.graphql.schema.type.connector.QLCustomConnector;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AppService;
import software.wings.settings.SettingValue;

import com.google.inject.Inject;
import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ConnectorDataFetcherTest extends AbstractDataFetcherTestBase {
  public static final String APPLICATION_NAME = "APPLICATION_NAME";
  @Inject ApplicationDataFetcher applicationDataFetcher;
  @Inject ConnectorDataFetcher connectorDataFetcher;
  @Inject AppService appService;

  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APPLICATION_NAME, TAG_TEAM, TAG_VALUE_TEAM1);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testConnectorDataFetcher() {
    SettingValue settingValue = CustomArtifactServerConfig.builder().accountId(ACCOUNT1_ID).build();
    createConnector(ACCOUNT1_ID, APP1_ID_ACCOUNT1, CONNECTOR_ID1_ACCOUNT1, "CONNECTOR_NAME", settingValue);

    QLConnector qlConnector = connectorDataFetcher.fetch(
        QLConnectorQueryParameters.builder().connectorId(CONNECTOR_ID1_ACCOUNT1).build(), ACCOUNT1_ID);
    assertThat(qlConnector).isInstanceOf(QLCustomConnector.class);
    QLCustomConnector customConnector = (QLCustomConnector) qlConnector;
    assertThat(customConnector.getId()).isEqualTo(CONNECTOR_ID1_ACCOUNT1);
    assertThat(customConnector.getName()).isEqualTo("CONNECTOR_NAME");
    assertThat(customConnector.getCreatedAt()).isCloseTo(System.currentTimeMillis(), within(60000L));

    assertThatThrownBy(
        ()
            -> connectorDataFetcher.fetch(
                QLConnectorQueryParameters.builder().connectorId(CONNECTOR_ID2_ACCOUNT1).build(), ACCOUNT1_ID))
        .isInstanceOf(InvalidRequestException.class);

    try {
      connectorDataFetcher.fetch(
          QLConnectorQueryParameters.builder().connectorId(CONNECTOR_ID1_ACCOUNT1).build(), ACCOUNT2_ID);
      fail("InvalidRequestException expected here");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(InvalidRequestException.class);
    }
  }
}
