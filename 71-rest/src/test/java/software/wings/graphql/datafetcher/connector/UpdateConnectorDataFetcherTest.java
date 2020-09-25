package software.wings.graphql.datafetcher.connector;

import static io.harness.rule.OwnerRule.MILOS;
import static io.harness.rule.OwnerRule.TMACARI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.ACCOUNT_ID;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.AUTHOR;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.BRANCH;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.CONNECTOR_ID;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.EMAIL;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.MESSAGE;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.PASSWORD;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.SSH;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.URL;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.USERNAME;
import static software.wings.graphql.datafetcher.connector.utils.Utility.getQlDockerConnectorInputBuilder;
import static software.wings.graphql.datafetcher.connector.utils.Utility.getQlGitConnectorInputBuilder;
import static software.wings.graphql.datafetcher.connector.utils.Utility.getQlNexusConnectorInputBuilder;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CONNECTORS;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.DockerConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.config.NexusConfig;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.connector.input.QLConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLCustomCommitDetailsInput;
import software.wings.graphql.schema.mutation.connector.input.QLDockerConnectorInput.QLDockerConnectorInputBuilder;
import software.wings.graphql.schema.mutation.connector.input.QLGitConnectorInput.QLGitConnectorInputBuilder;
import software.wings.graphql.schema.mutation.connector.input.QLNexusConnectorInput.QLNexusConnectorInputBuilder;
import software.wings.graphql.schema.mutation.connector.input.QLNexusVersion;
import software.wings.graphql.schema.mutation.connector.payload.QLUpdateConnectorPayload;
import software.wings.graphql.schema.type.QLConnectorType;
import software.wings.graphql.schema.type.connector.QLDockerConnector;
import software.wings.graphql.schema.type.connector.QLGitConnector;
import software.wings.graphql.schema.type.connector.QLNexusConnector;
import software.wings.security.annotations.AuthRule;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.lang.reflect.Method;
import java.sql.SQLException;

public class UpdateConnectorDataFetcherTest extends AbstractDataFetcherTest {
  @Mock private SettingsService settingsService;
  @Mock private SettingServiceHelper settingServiceHelper;
  @Mock private ConnectorsController connectorsController;
  @Mock private SecretManager secretManager;

  @InjectMocks @Inject private UpdateConnectorDataFetcher dataFetcher;

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void checkIfPermissionCorrect() throws NoSuchMethodException {
    Method method = UpdateConnectorDataFetcher.class.getDeclaredMethod(
        "mutateAndFetch", QLConnectorInput.class, MutationContext.class);
    AuthRule annotation = method.getAnnotation(AuthRule.class);
    assertThat(annotation.permissionType()).isEqualTo(MANAGE_CONNECTORS);
  }

  // UPDATE GIT CONNECTOR TESTS
  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void updateGitConnector() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withAccountId(ACCOUNT_ID)
                                   .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                                   .withValue(GitConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                 .withValue(GitConfig.builder().accountId(ACCOUNT_ID).build())
                 .build())
        .when(settingsService)
        .updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);

    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    doReturn(QLGitConnector.builder()).when(connectorsController).getConnectorBuilder(any());
    doReturn(QLGitConnector.builder()).when(connectorsController).populateConnector(any(), any());
    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);

    QLUpdateConnectorPayload payload = dataFetcher.mutateAndFetch(
        QLConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.GIT)
            .gitConnector(
                getQlGitConnectorInputBuilder()
                    .branch(RequestField.ofNullable(BRANCH))
                    .generateWebhookUrl(RequestField.ofNullable(true))
                    .customCommitDetails(RequestField.ofNullable(QLCustomCommitDetailsInput.builder()
                                                                     .authorName(RequestField.ofNullable(AUTHOR))
                                                                     .authorEmailId(RequestField.ofNullable(EMAIL))
                                                                     .commitMessage(RequestField.ofNullable(MESSAGE))
                                                                     .build()))
                    .passwordSecretId(RequestField.ofNullable(PASSWORD))
                    .build())
            .build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).getByAccount(ACCOUNT_ID, CONNECTOR_ID);
    verify(settingsService, times(1)).updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    assertThat(payload.getConnector()).isNotNull();
    assertThat(payload.getConnector()).isInstanceOf(QLGitConnector.class);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void updateGitWithBothSecrets() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                                   .withValue(GitConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorId(CONNECTOR_ID)
                                 .connectorType(QLConnectorType.GIT)
                                 .gitConnector(getQlGitConnectorInputBuilder()
                                                   .passwordSecretId(RequestField.ofNullable(PASSWORD))
                                                   .sshSettingId(RequestField.ofNullable(SSH))
                                                   .build())
                                 .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Just one secretId should be specified");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void updateGitWithNonExistentSecretId() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                                   .withValue(GitConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    QLConnectorInput input =
        QLConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.GIT)
            .gitConnector(getQlGitConnectorInputBuilder().sshSettingId(RequestField.ofNullable(SSH)).build())
            .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Secret does not exist");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void updateGitWithPasswordSecretWhenNoUsername() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                                   .withValue(GitConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    QLGitConnectorInputBuilder updateGitConnectorInputBuilder =
        getQlGitConnectorInputBuilder()
            .userName(RequestField.absent())
            .passwordSecretId(RequestField.ofNullable(PASSWORD));

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorId(CONNECTOR_ID)
                                 .connectorType(QLConnectorType.GIT)
                                 .gitConnector(updateGitConnectorInputBuilder.build())
                                 .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("userName should be specified");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void updateGitDifferentSettingCategoryReturned() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                                   .withValue(GitConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorId(CONNECTOR_ID)
                                 .connectorType(QLConnectorType.GIT)
                                 .gitConnector(getQlGitConnectorInputBuilder().build())
                                 .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("No connector exists with the connectorId ".concat(CONNECTOR_ID));
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void updateGitDifferentConnectorType() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                                   .withValue(GitConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorId(CONNECTOR_ID)
                                 .connectorType(QLConnectorType.ARTIFACTORY)
                                 .gitConnector(getQlGitConnectorInputBuilder().build())
                                 .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(
            "The existing connector is of type GIT and the update operation inputs a connector of type ARTIFACTORY");
  }

  // UPDATE DOCKER CONNECTOR TESTS
  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateDockerConnector() {
    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute()
            .withAccountId(ACCOUNT_ID)
            .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
            .withValue(DockerConfig.builder().accountId(ACCOUNT_ID).dockerRegistryUrl(URL).build())
            .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                 .withValue(DockerConfig.builder().accountId(ACCOUNT_ID).dockerRegistryUrl(URL).build())
                 .build())
        .when(settingsService)
        .updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);

    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    doReturn(QLDockerConnector.builder()).when(connectorsController).getConnectorBuilder(any());
    doReturn(QLDockerConnector.builder()).when(connectorsController).populateConnector(any(), any());
    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);

    QLUpdateConnectorPayload payload = dataFetcher.mutateAndFetch(
        QLConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.DOCKER)
            .dockerConnector(
                getQlDockerConnectorInputBuilder().passwordSecretId(RequestField.ofNullable(PASSWORD)).build())
            .build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).getByAccount(ACCOUNT_ID, CONNECTOR_ID);
    verify(settingsService, times(1)).updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    assertThat(payload.getConnector()).isNotNull();
    assertThat(payload.getConnector()).isInstanceOf(QLDockerConnector.class);
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateDockerConnectorWithoutUsername() {
    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute()
            .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
            .withValue(DockerConfig.builder().accountId(ACCOUNT_ID).dockerRegistryUrl(URL).build())
            .build();

    QLDockerConnectorInputBuilder updateDockerConnectorInputBuilder =
        getQlDockerConnectorInputBuilder()
            .userName(RequestField.absent())
            .passwordSecretId(RequestField.ofNullable(PASSWORD));

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorId(CONNECTOR_ID)
                                 .connectorType(QLConnectorType.DOCKER)
                                 .dockerConnector(updateDockerConnectorInputBuilder.build())
                                 .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("userName should be specified");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateDockerConnectorWithoutConnectorType() {
    QLDockerConnectorInputBuilder updateDockerConnectorInputBuilder =
        getQlDockerConnectorInputBuilder()
            .userName(RequestField.ofNullable(USERNAME))
            .passwordSecretId(RequestField.ofNullable(PASSWORD));

    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorId(CONNECTOR_ID)
                                 .connectorType(null)
                                 .dockerConnector(updateDockerConnectorInputBuilder.build())
                                 .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Invalid connector type provided");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateDockerConnectorWithoutConnectorID() {
    QLDockerConnectorInputBuilder updateDockerConnectorInputBuilder =
        getQlDockerConnectorInputBuilder()
            .userName(RequestField.ofNullable(USERNAME))
            .passwordSecretId(RequestField.ofNullable(PASSWORD));

    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorId(null)
                                 .connectorType(QLConnectorType.DOCKER)
                                 .dockerConnector(updateDockerConnectorInputBuilder.build())
                                 .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Connector ID is not provided");
  }

  // UPDATE NEXUS CONNECTOR TESTS
  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateNexusConnector() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withAccountId(ACCOUNT_ID)
                                   .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                                   .withValue(NexusConfig.builder().accountId(ACCOUNT_ID).nexusUrl(URL).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                 .withValue(NexusConfig.builder().accountId(ACCOUNT_ID).nexusUrl(URL).build())
                 .build())
        .when(settingsService)
        .updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);

    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    doReturn(QLNexusConnector.builder()).when(connectorsController).getConnectorBuilder(any());
    doReturn(QLNexusConnector.builder()).when(connectorsController).populateConnector(any(), any());
    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);

    QLUpdateConnectorPayload payload =
        dataFetcher.mutateAndFetch(QLConnectorInput.builder()
                                       .connectorId(CONNECTOR_ID)
                                       .connectorType(QLConnectorType.NEXUS)
                                       .nexusConnector(getQlNexusConnectorInputBuilder()
                                                           .passwordSecretId(RequestField.ofNullable(PASSWORD))
                                                           .version(RequestField.ofNullable(QLNexusVersion.V2))
                                                           .build())
                                       .build(),
            MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).getByAccount(ACCOUNT_ID, CONNECTOR_ID);
    verify(settingsService, times(1)).updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    assertThat(payload.getConnector()).isNotNull();
    assertThat(payload.getConnector()).isInstanceOf(QLNexusConnector.class);
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateNexusConnectorWithoutUsername() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                                   .withValue(NexusConfig.builder().accountId(ACCOUNT_ID).nexusUrl(URL).build())
                                   .build();

    QLNexusConnectorInputBuilder updateNexusConnectorInputBuilder =
        getQlNexusConnectorInputBuilder()
            .userName(RequestField.absent())
            .passwordSecretId(RequestField.ofNullable(PASSWORD));

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorId(CONNECTOR_ID)
                                 .connectorType(QLConnectorType.NEXUS)
                                 .nexusConnector(updateNexusConnectorInputBuilder.build())
                                 .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("userName should be specified");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateNexusConnectorWithoutConnectorType() {
    QLNexusConnectorInputBuilder updateNexusConnectorInputBuilder =
        getQlNexusConnectorInputBuilder()
            .userName(RequestField.ofNullable(USERNAME))
            .passwordSecretId(RequestField.ofNullable(PASSWORD));

    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorId(CONNECTOR_ID)
                                 .connectorType(null)
                                 .nexusConnector(updateNexusConnectorInputBuilder.build())
                                 .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Invalid connector type provided");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateNexusConnectorWithoutConnectorID() {
    QLNexusConnectorInputBuilder updateNexusConnectorInputBuilder =
        getQlNexusConnectorInputBuilder()
            .userName(RequestField.ofNullable(USERNAME))
            .passwordSecretId(RequestField.ofNullable(PASSWORD));

    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorId(null)
                                 .connectorType(QLConnectorType.DOCKER)
                                 .nexusConnector(updateNexusConnectorInputBuilder.build())
                                 .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Connector ID is not provided");
  }
}
