package software.wings.graphql.datafetcher.connector.types;

import static io.harness.rule.OwnerRule.TMACARI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.connector.ConnectorsController;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.type.QLConnectorType;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ConnectorFactoryTest {
  @Mock private SecretManager secretManager;
  @Mock private SettingsService settingsService;
  @Mock private ConnectorsController connectorsController;
  @Mock private UsageScopeController usageScopeController;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetGitConnector() {
    Connector connector = ConnectorFactory.getConnector(
        QLConnectorType.GIT, connectorsController, secretManager, settingsService, usageScopeController);
    assertThat(connector).isInstanceOf(GitConnector.class);
  }
}
