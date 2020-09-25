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
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.ACCOUNT_ID;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.AUTHOR;
import static software.wings.graphql.datafetcher.connector.utils.ConnectorConstants.BRANCH;
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
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.connector.input.QLConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLCustomCommitDetailsInput;
import software.wings.graphql.schema.mutation.connector.input.QLDockerConnectorInput.QLDockerConnectorInputBuilder;
import software.wings.graphql.schema.mutation.connector.input.QLGitConnectorInput.QLGitConnectorInputBuilder;
import software.wings.graphql.schema.mutation.connector.input.QLNexusConnectorInput.QLNexusConnectorInputBuilder;
import software.wings.graphql.schema.mutation.connector.input.QLNexusVersion;
import software.wings.graphql.schema.mutation.connector.payload.QLCreateConnectorPayload;
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

public class CreateConnectorDataFetcherTest {
  @Mock private SettingsService settingsService;
  @Mock private SettingServiceHelper settingServiceHelper;
  @Mock private ConnectorsController connectorsController;
  @Mock private SecretManager secretManager;

  @InjectMocks private CreateConnectorDataFetcher dataFetcher = new CreateConnectorDataFetcher();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void checkIfPermissionCorrect() throws NoSuchMethodException {
    Method method = CreateConnectorDataFetcher.class.getDeclaredMethod(
        "mutateAndFetch", QLConnectorInput.class, MutationContext.class);
    AuthRule annotation = method.getAnnotation(AuthRule.class);
    assertThat(annotation.permissionType()).isEqualTo(MANAGE_CONNECTORS);
  }

  // CREATE GIT CONNECTOR TESTS
  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void createGitConnector() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                                   .withValue(GitConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting)
        .when(settingsService)
        .saveWithPruning(isA(SettingAttribute.class), isA(String.class), isA(String.class));
    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));
    doReturn(QLGitConnector.builder()).when(connectorsController).getConnectorBuilder(any());
    doReturn(QLGitConnector.builder()).when(connectorsController).populateConnector(any(), any());
    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);

    QLCreateConnectorPayload payload = dataFetcher.mutateAndFetch(
        QLConnectorInput.builder()
            .connectorType(QLConnectorType.GIT)
            .gitConnector(
                getQlGitConnectorInputBuilder()
                    .branch(RequestField.ofNullable(BRANCH))
                    .generateWebhookUrl(RequestField.ofNullable(true))
                    .urlType(RequestField.ofNullable(GitConfig.UrlType.REPO))
                    .customCommitDetails(RequestField.ofNullable(QLCustomCommitDetailsInput.builder()
                                                                     .authorName(RequestField.ofNullable(AUTHOR))
                                                                     .authorEmailId(RequestField.ofNullable(EMAIL))
                                                                     .commitMessage(RequestField.ofNullable(MESSAGE))
                                                                     .build()))
                    .passwordSecretId(RequestField.ofNullable(PASSWORD))
                    .build())
            .build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1))
        .saveWithPruning(isA(SettingAttribute.class), isA(String.class), isA(String.class));
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));
    assertThat(payload.getConnector()).isNotNull();
    assertThat(payload.getConnector()).isInstanceOf(QLGitConnector.class);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void createGitConnectorSpecifyingBothSecrets() {
    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorType(QLConnectorType.GIT)
                                 .gitConnector(getQlGitConnectorInputBuilder()
                                                   .passwordSecretId(RequestField.ofNullable(PASSWORD))
                                                   .sshSettingId(RequestField.ofNullable(SSH))
                                                   .build())
                                 .build();
    MutationContext mutationContext = MutationContext.builder().accountId(ACCOUNT_ID).build();
    doReturn(new SettingAttribute()).when(settingsService).getByAccount(ACCOUNT_ID, PASSWORD);
    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Just one secretId should be specified");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void createGitConnectorNotSpecifyingSecrets() {
    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorType(QLConnectorType.GIT)
                                 .gitConnector(getQlGitConnectorInputBuilder().build())
                                 .build();
    MutationContext mutationContext = MutationContext.builder().accountId(ACCOUNT_ID).build();
    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("No secretId provided with the request for connector");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void createGitConnectorWithPasswordNotSpecifyingUsername() {
    QLGitConnectorInputBuilder gitConnectorInputBuilder = getQlGitConnectorInputBuilder();
    gitConnectorInputBuilder.userName(RequestField.absent()).passwordSecretId(RequestField.ofNullable(PASSWORD));
    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorType(QLConnectorType.GIT)
                                 .gitConnector(gitConnectorInputBuilder.build())
                                 .build();
    MutationContext mutationContext = MutationContext.builder().accountId(ACCOUNT_ID).build();
    doReturn(new SettingAttribute()).when(settingsService).getByAccount(ACCOUNT_ID, PASSWORD);

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("userName should be specified");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void createGitConnectorWitNonExistentSecretId() {
    QLGitConnectorInputBuilder gitConnectorInputBuilder = getQlGitConnectorInputBuilder();
    gitConnectorInputBuilder.userName(RequestField.ofNullable(USERNAME))
        .passwordSecretId(RequestField.ofNullable(PASSWORD));
    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorType(QLConnectorType.GIT)
                                 .gitConnector(gitConnectorInputBuilder.build())
                                 .build();
    MutationContext mutationContext = MutationContext.builder().accountId(ACCOUNT_ID).build();
    doReturn(null).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);
    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Secret does not exist");
  }

  // CREATE DOCKER CONNECTOR TESTS
  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void createDockerConnector() {
    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute()
            .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
            .withValue(DockerConfig.builder().accountId(ACCOUNT_ID).dockerRegistryUrl(URL).build())
            .build();

    doReturn(setting)
        .when(settingsService)
        .saveWithPruning(isA(SettingAttribute.class), isA(String.class), isA(String.class));
    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));
    doReturn(QLDockerConnector.builder()).when(connectorsController).getConnectorBuilder(any());
    doReturn(QLDockerConnector.builder()).when(connectorsController).populateConnector(any(), any());
    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);

    QLCreateConnectorPayload payload = dataFetcher.mutateAndFetch(
        QLConnectorInput.builder()
            .connectorType(QLConnectorType.DOCKER)
            .dockerConnector(
                getQlDockerConnectorInputBuilder().passwordSecretId(RequestField.ofNullable(PASSWORD)).build())
            .build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1))
        .saveWithPruning(isA(SettingAttribute.class), isA(String.class), isA(String.class));
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));
    assertThat(payload.getConnector()).isNotNull();
    assertThat(payload.getConnector()).isInstanceOf(QLDockerConnector.class);
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void createDockerConnectorWithoutUsername() {
    QLDockerConnectorInputBuilder dockerConnectorInputBuilder = getQlDockerConnectorInputBuilder();
    dockerConnectorInputBuilder.userName(RequestField.absent()).passwordSecretId(RequestField.ofNullable(PASSWORD));
    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorType(QLConnectorType.DOCKER)
                                 .dockerConnector(dockerConnectorInputBuilder.build())
                                 .build();
    MutationContext mutationContext = MutationContext.builder().accountId(ACCOUNT_ID).build();
    doReturn(new SettingAttribute()).when(settingsService).getByAccount(ACCOUNT_ID, PASSWORD);

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("userName should be specified");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void createDockerConnectorWithEmptyUsername() {
    QLDockerConnectorInputBuilder dockerConnectorInputBuilder = getQlDockerConnectorInputBuilder();
    dockerConnectorInputBuilder.userName(RequestField.ofNullable(" "))
        .passwordSecretId(RequestField.ofNullable(PASSWORD));
    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorType(QLConnectorType.DOCKER)
                                 .dockerConnector(dockerConnectorInputBuilder.build())
                                 .build();
    MutationContext mutationContext = MutationContext.builder().accountId(ACCOUNT_ID).build();
    doReturn(new SettingAttribute()).when(settingsService).getByAccount(ACCOUNT_ID, PASSWORD);

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("userName should be specified");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void createDockerConnectorWithEmptyURL() {
    QLDockerConnectorInputBuilder dockerConnectorInputBuilder = getQlDockerConnectorInputBuilder();
    dockerConnectorInputBuilder.userName(RequestField.ofNullable(USERNAME))
        .URL(RequestField.ofNullable(" "))
        .passwordSecretId(RequestField.ofNullable(PASSWORD));
    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorType(QLConnectorType.DOCKER)
                                 .dockerConnector(dockerConnectorInputBuilder.build())
                                 .build();
    MutationContext mutationContext = MutationContext.builder().accountId(ACCOUNT_ID).build();
    doReturn(new SettingAttribute()).when(settingsService).getByAccount(ACCOUNT_ID, PASSWORD);
    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("URL should be specified");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void createDockerConnectorWithoutConnectorType() {
    QLDockerConnectorInputBuilder dockerConnectorInputBuilder = getQlDockerConnectorInputBuilder();
    dockerConnectorInputBuilder.userName(RequestField.ofNullable(USERNAME))
        .passwordSecretId(RequestField.ofNullable(PASSWORD));
    QLConnectorInput input =
        QLConnectorInput.builder().connectorType(null).dockerConnector(dockerConnectorInputBuilder.build()).build();
    MutationContext mutationContext = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Invalid connector type provided");
  }

  // CREATE NEXUS CONNECTOR TESTS
  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void createNexusConnector() {
    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute()
            .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
            .withValue(DockerConfig.builder().accountId(ACCOUNT_ID).dockerRegistryUrl(URL).build())
            .build();

    doReturn(setting)
        .when(settingsService)
        .saveWithPruning(isA(SettingAttribute.class), isA(String.class), isA(String.class));
    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));
    doReturn(QLNexusConnector.builder()).when(connectorsController).getConnectorBuilder(any());
    doReturn(QLNexusConnector.builder()).when(connectorsController).populateConnector(any(), any());
    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);

    QLCreateConnectorPayload payload =
        dataFetcher.mutateAndFetch(QLConnectorInput.builder()
                                       .connectorType(QLConnectorType.NEXUS)
                                       .nexusConnector(getQlNexusConnectorInputBuilder()
                                                           .passwordSecretId(RequestField.ofNullable(PASSWORD))
                                                           .version(RequestField.ofNullable(QLNexusVersion.V2))
                                                           .build())
                                       .build(),
            MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1))
        .saveWithPruning(isA(SettingAttribute.class), isA(String.class), isA(String.class));
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));
    assertThat(payload.getConnector()).isNotNull();
    assertThat(payload.getConnector()).isInstanceOf(QLNexusConnector.class);
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void createNexusConnectorWithoutUsername() {
    QLNexusConnectorInputBuilder nexusConnectorInputBuilder = getQlNexusConnectorInputBuilder();
    nexusConnectorInputBuilder.userName(RequestField.absent())
        .passwordSecretId(RequestField.ofNullable(PASSWORD))
        .version(RequestField.ofNullable(QLNexusVersion.V2));

    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorType(QLConnectorType.NEXUS)
                                 .nexusConnector(nexusConnectorInputBuilder.build())
                                 .build();
    MutationContext mutationContext = MutationContext.builder().accountId(ACCOUNT_ID).build();
    doReturn(new SettingAttribute()).when(settingsService).getByAccount(ACCOUNT_ID, PASSWORD);

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("userName should be specified");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void createNexusConnectorWithEmptyUsername() {
    QLNexusConnectorInputBuilder nexusConnectorInputBuilder = getQlNexusConnectorInputBuilder();
    nexusConnectorInputBuilder.userName(RequestField.ofNullable(" "))
        .passwordSecretId(RequestField.ofNullable(PASSWORD))
        .version(RequestField.ofNullable(QLNexusVersion.V2));

    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorType(QLConnectorType.NEXUS)
                                 .nexusConnector(nexusConnectorInputBuilder.build())
                                 .build();
    MutationContext mutationContext = MutationContext.builder().accountId(ACCOUNT_ID).build();
    doReturn(new SettingAttribute()).when(settingsService).getByAccount(ACCOUNT_ID, PASSWORD);

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("userName should be specified");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void createNexusConnectorWithEmptyURL() {
    QLNexusConnectorInputBuilder nexusConnectorInputBuilder = getQlNexusConnectorInputBuilder();
    nexusConnectorInputBuilder.userName(RequestField.ofNullable(USERNAME))
        .passwordSecretId(RequestField.ofNullable(PASSWORD))
        .URL(RequestField.ofNullable(" "))
        .version(RequestField.ofNullable(QLNexusVersion.V2));

    QLConnectorInput input = QLConnectorInput.builder()
                                 .connectorType(QLConnectorType.NEXUS)
                                 .nexusConnector(nexusConnectorInputBuilder.build())
                                 .build();
    MutationContext mutationContext = MutationContext.builder().accountId(ACCOUNT_ID).build();
    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);
    doReturn(new SettingAttribute()).when(settingsService).getByAccount(ACCOUNT_ID, PASSWORD);

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("URL should be specified");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void createNexusConnectorWithoutConnectorType() {
    QLNexusConnectorInputBuilder nexusConnectorInputBuilder = getQlNexusConnectorInputBuilder();
    nexusConnectorInputBuilder.userName(RequestField.ofNullable(USERNAME))
        .passwordSecretId(RequestField.ofNullable(PASSWORD))
        .version(RequestField.ofNullable(QLNexusVersion.V2));
    QLConnectorInput input =
        QLConnectorInput.builder().connectorType(null).nexusConnector(nexusConnectorInputBuilder.build()).build();
    MutationContext mutationContext = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Invalid connector type provided");
  }
}
