package software.wings.graphql.datafetcher.connector.types;

import static io.harness.rule.OwnerRule.TMACARI;

import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.ACCOUNT_ID;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.AUTHOR;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.BRANCH;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.EMAIL;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.MESSAGE;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.PASSWORD;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.SSH;
import static software.wings.graphql.datafetcher.connector.utils.Utility.getQlGitConnectorInputBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.beans.EncryptedData;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;

import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.connector.ConnectorsController;
import software.wings.graphql.schema.mutation.connector.input.QLConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.git.QLCustomCommitDetailsInput;
import software.wings.graphql.schema.mutation.connector.input.git.QLGitConnectorInput;
import software.wings.graphql.schema.type.connector.QLGitConnector;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GitConnectorTest {
  @Mock private SecretManager secretManager;
  @Mock private SettingsService settingsService;
  @Mock private ConnectorsController connectorsController;

  @InjectMocks private GitConnector gitConnector = new GitConnector();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetSettingAttributeWithSshSettingId() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute().build();
    doReturn(setting).when(settingsService).getByAccount(any(), any());
    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);
    doReturn(QLGitConnector.builder()).when(connectorsController).getConnectorBuilder(any());
    doReturn(QLGitConnector.builder()).when(connectorsController).populateConnector(any(), any());

    QLGitConnectorInput qlGitConnectorInput =
        getQlGitConnectorInputBuilder()
            .branch(RequestField.ofNullable(BRANCH))
            .generateWebhookUrl(RequestField.ofNullable(true))
            .urlType(RequestField.ofNullable(GitConfig.UrlType.REPO))
            .customCommitDetails(RequestField.ofNullable(QLCustomCommitDetailsInput.builder()
                                                             .authorName(RequestField.ofNullable(AUTHOR))
                                                             .authorEmailId(RequestField.ofNullable(EMAIL))
                                                             .commitMessage(RequestField.ofNullable(MESSAGE))
                                                             .build()))
            .sshSettingId(RequestField.ofNullable(SSH))
            .passwordSecretId(RequestField.ofNull())
            .build();

    SettingAttribute settingAttribute = gitConnector.getSettingAttribute(
        QLConnectorInput.builder().gitConnector(qlGitConnectorInput).build(), ACCOUNT_ID);

    assertThat(((GitConfig) settingAttribute.getValue()).getSshSettingId()).isEqualTo(SSH);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetSettingAttributeWithPasswordSecretId() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute().build();
    doReturn(setting).when(settingsService).getByAccount(any(), any());
    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);
    doReturn(QLGitConnector.builder()).when(connectorsController).getConnectorBuilder(any());
    doReturn(QLGitConnector.builder()).when(connectorsController).populateConnector(any(), any());

    QLGitConnectorInput qlGitConnectorInput =
        getQlGitConnectorInputBuilder()
            .branch(RequestField.ofNullable(BRANCH))
            .generateWebhookUrl(RequestField.ofNullable(true))
            .urlType(RequestField.ofNullable(GitConfig.UrlType.REPO))
            .customCommitDetails(RequestField.ofNullable(QLCustomCommitDetailsInput.builder()
                                                             .authorName(RequestField.ofNullable(AUTHOR))
                                                             .authorEmailId(RequestField.ofNullable(EMAIL))
                                                             .commitMessage(RequestField.ofNullable(MESSAGE))
                                                             .build()))
            .sshSettingId(RequestField.ofNull())
            .passwordSecretId(RequestField.ofNullable(PASSWORD))
            .build();

    SettingAttribute settingAttribute = gitConnector.getSettingAttribute(
        QLConnectorInput.builder().gitConnector(qlGitConnectorInput).build(), ACCOUNT_ID);

    assertThat(((GitConfig) settingAttribute.getValue()).getEncryptedPassword()).isEqualTo(PASSWORD);
  }
}
