package software.wings.graphql.datafetcher.connector;

import static io.harness.rule.OwnerRule.MILOS;
import static io.harness.rule.OwnerRule.PRABU;
import static io.harness.rule.OwnerRule.TMACARI;

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
import static software.wings.graphql.datafetcher.connector.utils.Utility.getQlAmazonS3PlatformInputBuilder;
import static software.wings.graphql.datafetcher.connector.utils.Utility.getQlDockerConnectorInputBuilder;
import static software.wings.graphql.datafetcher.connector.utils.Utility.getQlGCSPlatformInputBuilder;
import static software.wings.graphql.datafetcher.connector.utils.Utility.getQlHelmConnectorInputBuilder;
import static software.wings.graphql.datafetcher.connector.utils.Utility.getQlHttpServerPlatformInputBuilder;
import static software.wings.graphql.datafetcher.connector.utils.Utility.getQlNexusConnectorInputBuilder;
import static software.wings.graphql.datafetcher.connector.utils.Utility.getQlUpdateGitConnectorInputBuilder;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CONNECTORS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.beans.EncryptedData;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;

import software.wings.beans.DockerConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.datafetcher.connector.utils.Utility;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.connector.input.QLUpdateConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.docker.QLDockerConnectorInput.QLDockerConnectorInputBuilder;
import software.wings.graphql.schema.mutation.connector.input.git.QLCustomCommitDetailsInput;
import software.wings.graphql.schema.mutation.connector.input.git.QLUpdateGitConnectorInput.QLUpdateGitConnectorInputBuilder;
import software.wings.graphql.schema.mutation.connector.input.nexus.QLNexusConnectorInput.QLNexusConnectorInputBuilder;
import software.wings.graphql.schema.mutation.connector.input.nexus.QLNexusVersion;
import software.wings.graphql.schema.mutation.connector.payload.QLUpdateConnectorPayload;
import software.wings.graphql.schema.type.QLConnectorType;
import software.wings.graphql.schema.type.connector.QLAmazonS3HelmRepoConnector;
import software.wings.graphql.schema.type.connector.QLDockerConnector;
import software.wings.graphql.schema.type.connector.QLGCSHelmRepoConnector;
import software.wings.graphql.schema.type.connector.QLGitConnector;
import software.wings.graphql.schema.type.connector.QLHttpHelmRepoConnector;
import software.wings.graphql.schema.type.connector.QLNexusConnector;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import javax.validation.ConstraintViolationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class UpdateConnectorDataFetcherTest extends AbstractDataFetcherTestBase {
  @Mock private SettingsService settingsService;
  @Mock private SettingServiceHelper settingServiceHelper;
  @Mock private ConnectorsController connectorsController;
  @Mock private SecretManager secretManager;
  @Mock private UsageScopeController usageScopeController;

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
        "mutateAndFetch", QLUpdateConnectorInput.class, MutationContext.class);
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
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.GIT)
            .gitConnector(
                getQlUpdateGitConnectorInputBuilder()
                    .branch(RequestField.ofNullable(BRANCH))
                    .generateWebhookUrl(RequestField.ofNullable(true))
                    .customCommitDetails(RequestField.ofNullable(QLCustomCommitDetailsInput.builder()
                                                                     .authorName(RequestField.ofNullable(AUTHOR))
                                                                     .authorEmailId(RequestField.ofNullable(EMAIL))
                                                                     .commitMessage(RequestField.ofNullable(MESSAGE))
                                                                     .build()))
                    .delegateSelectors(RequestField.ofNull())
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
  public void updateShhGitConnector() {
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
    doReturn(new SettingAttribute()).when(settingsService).getByAccount(ACCOUNT_ID, SSH);

    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    doReturn(QLGitConnector.builder()).when(connectorsController).getConnectorBuilder(any());
    doReturn(QLGitConnector.builder()).when(connectorsController).populateConnector(any(), any());

    QLUpdateConnectorPayload payload = dataFetcher.mutateAndFetch(
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.GIT)
            .gitConnector(
                getQlUpdateGitConnectorInputBuilder()
                    .branch(RequestField.ofNullable(BRANCH))
                    .generateWebhookUrl(RequestField.ofNullable(true))
                    .customCommitDetails(RequestField.ofNullable(QLCustomCommitDetailsInput.builder()
                                                                     .authorName(RequestField.ofNullable(AUTHOR))
                                                                     .authorEmailId(RequestField.ofNullable(EMAIL))
                                                                     .commitMessage(RequestField.ofNullable(MESSAGE))
                                                                     .build()))
                    .delegateSelectors(RequestField.ofNull())
                    .sshSettingId(RequestField.ofNullable(SSH))
                    .usageScope(RequestField.ofNullable(QLUsageScope.builder().build()))
                    .build())
            .build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(usageScopeController, times(1)).populateUsageRestrictions(any(), any());
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

    QLUpdateConnectorInput input = QLUpdateConnectorInput.builder()
                                       .connectorId(CONNECTOR_ID)
                                       .connectorType(QLConnectorType.GIT)
                                       .gitConnector(getQlUpdateGitConnectorInputBuilder()
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

    QLUpdateConnectorInput input =
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.GIT)
            .gitConnector(getQlUpdateGitConnectorInputBuilder().sshSettingId(RequestField.ofNullable(SSH)).build())
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

    QLUpdateGitConnectorInputBuilder updateGitConnectorInputBuilder =
        getQlUpdateGitConnectorInputBuilder()
            .userName(RequestField.absent())
            .passwordSecretId(RequestField.ofNullable(PASSWORD));

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    QLUpdateConnectorInput input = QLUpdateConnectorInput.builder()
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

    QLUpdateConnectorInput input = QLUpdateConnectorInput.builder()
                                       .connectorId(CONNECTOR_ID)
                                       .connectorType(QLConnectorType.GIT)
                                       .gitConnector(getQlUpdateGitConnectorInputBuilder().build())
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

    QLUpdateConnectorInput input = QLUpdateConnectorInput.builder()
                                       .connectorId(CONNECTOR_ID)
                                       .connectorType(QLConnectorType.ARTIFACTORY)
                                       .gitConnector(getQlUpdateGitConnectorInputBuilder().build())
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
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withAccountId(ACCOUNT_ID)
                                   .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                                   .withValue(DockerConfig.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .dockerRegistryUrl(URL)
                                                  .delegateSelectors(Collections.singletonList("delegateSelector"))
                                                  .build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                 .withValue(DockerConfig.builder()
                                .accountId(ACCOUNT_ID)
                                .dockerRegistryUrl(URL)
                                .delegateSelectors(Collections.singletonList("delegateSelector"))
                                .build())
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
        QLUpdateConnectorInput.builder()
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
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                                   .withValue(DockerConfig.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .dockerRegistryUrl(URL)
                                                  .delegateSelectors(Collections.singletonList("delegateSelector"))
                                                  .build())
                                   .build();

    QLDockerConnectorInputBuilder updateDockerConnectorInputBuilder =
        getQlDockerConnectorInputBuilder()
            .userName(RequestField.absent())
            .passwordSecretId(RequestField.ofNullable(PASSWORD));

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    QLUpdateConnectorInput input = QLUpdateConnectorInput.builder()
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

    QLUpdateConnectorInput input = QLUpdateConnectorInput.builder()
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

    QLUpdateConnectorInput input = QLUpdateConnectorInput.builder()
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
        dataFetcher.mutateAndFetch(QLUpdateConnectorInput.builder()
                                       .connectorId(CONNECTOR_ID)
                                       .connectorType(QLConnectorType.NEXUS)
                                       .nexusConnector(getQlNexusConnectorInputBuilder()
                                                           .delegateSelectors(RequestField.ofNull())
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

    QLUpdateConnectorInput input = QLUpdateConnectorInput.builder()
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

    QLUpdateConnectorInput input = QLUpdateConnectorInput.builder()
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

    QLUpdateConnectorInput input = QLUpdateConnectorInput.builder()
                                       .connectorId(null)
                                       .connectorType(QLConnectorType.NEXUS)
                                       .nexusConnector(updateNexusConnectorInputBuilder.build())
                                       .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Connector ID is not provided");
  }

  // UPDATE HELM CONNECTOR TESTS
  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateHelmHttpServerConnector() {
    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute()
            .withAccountId(ACCOUNT_ID)
            .withCategory(SettingAttribute.SettingCategory.HELM_REPO)
            .withValue(HttpHelmRepoConfig.builder().accountId(ACCOUNT_ID).chartRepoUrl(URL).build())
            .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withCategory(SettingAttribute.SettingCategory.HELM_REPO)
                 .withValue(HttpHelmRepoConfig.builder().accountId(ACCOUNT_ID).chartRepoUrl(URL).build())
                 .build())
        .when(settingsService)
        .updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);

    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    doReturn(QLHttpHelmRepoConnector.builder()).when(connectorsController).getConnectorBuilder(any());
    doReturn(QLHttpHelmRepoConnector.builder()).when(connectorsController).populateConnector(any(), any());
    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);

    QLUpdateConnectorPayload payload = dataFetcher.mutateAndFetch(
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.HTTP_HELM_REPO)
            .helmConnector(Utility
                               .getQlHelmConnectorInputBuilder(getQlHttpServerPlatformInputBuilder()
                                                                   .passwordSecretId(RequestField.ofNullable(PASSWORD))
                                                                   .build())
                               .build())
            .build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).getByAccount(ACCOUNT_ID, CONNECTOR_ID);
    verify(settingsService, times(1)).updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    assertThat(payload.getConnector()).isNotNull();
    assertThat(payload.getConnector()).isInstanceOf(QLHttpHelmRepoConnector.class);
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateHelmHttpServerConnectorWithoutUsername() {
    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute()
            .withCategory(SettingAttribute.SettingCategory.HELM_REPO)
            .withValue(HttpHelmRepoConfig.builder().accountId(ACCOUNT_ID).chartRepoUrl(URL).build())
            .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    QLUpdateConnectorInput input =
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.HTTP_HELM_REPO)
            .helmConnector(Utility
                               .getQlHelmConnectorInputBuilder(getQlHttpServerPlatformInputBuilder()
                                                                   .userName(RequestField.absent())
                                                                   .passwordSecretId(RequestField.ofNullable(PASSWORD))
                                                                   .build())
                               .build())
            .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("userName is not specified");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateHelmHttpServerConnectorWithoutConnectorType() {
    QLUpdateConnectorInput input =
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(null)
            .helmConnector(
                Utility.getQlHelmConnectorInputBuilder(getQlHttpServerPlatformInputBuilder().build()).build())
            .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Invalid connector type provided");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateHelmHttpServerConnectorWithoutConnectorID() {
    QLUpdateConnectorInput input =
        QLUpdateConnectorInput.builder()
            .connectorId(null)
            .connectorType(QLConnectorType.HTTP_HELM_REPO)
            .helmConnector(
                Utility.getQlHelmConnectorInputBuilder(getQlHttpServerPlatformInputBuilder().build()).build())
            .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Connector ID is not provided");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateHelmGCSConnector() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withAccountId(ACCOUNT_ID)
                                   .withCategory(SettingAttribute.SettingCategory.HELM_REPO)
                                   .withValue(GCSHelmRepoConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withCategory(SettingAttribute.SettingCategory.HELM_REPO)
                 .withValue(GCSHelmRepoConfig.builder().accountId(ACCOUNT_ID).build())
                 .build())
        .when(settingsService)
        .updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);

    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    doReturn(QLGCSHelmRepoConnector.builder()).when(connectorsController).getConnectorBuilder(any());
    doReturn(QLGCSHelmRepoConnector.builder()).when(connectorsController).populateConnector(any(), any());
    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);
    doReturn(setting).when(settingsService).getByAccountAndId(ACCOUNT_ID, "GCP");

    QLUpdateConnectorPayload payload = dataFetcher.mutateAndFetch(
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.GCS_HELM_REPO)
            .helmConnector(
                Utility
                    .getQlHelmConnectorInputBuilder(
                        getQlGCSPlatformInputBuilder().bucketName(RequestField.ofNullable("newBucketName")).build())
                    .build())
            .build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).getByAccount(ACCOUNT_ID, CONNECTOR_ID);
    verify(settingsService, times(1)).updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    assertThat(payload.getConnector()).isNotNull();
    assertThat(payload.getConnector()).isInstanceOf(QLGCSHelmRepoConnector.class);
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateHelmGCSConnectorWithoutProvider() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.HELM_REPO)
                                   .withValue(GCSHelmRepoConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    QLUpdateConnectorInput input =
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.GCS_HELM_REPO)
            .helmConnector(Utility
                               .getQlHelmConnectorInputBuilder(
                                   getQlGCSPlatformInputBuilder().googleCloudProvider(RequestField.absent()).build())
                               .build())
            .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Google Cloud provider is not specified for GCS hosting platform");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateHelmGCSConnectorWithoutBucketName() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withAccountId(ACCOUNT_ID)
                                   .withCategory(SettingAttribute.SettingCategory.HELM_REPO)
                                   .withValue(GCSHelmRepoConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);
    doReturn(setting).when(settingsService).getByAccountAndId(ACCOUNT_ID, "GCP");

    QLUpdateConnectorInput input =
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.GCS_HELM_REPO)
            .helmConnector(Utility
                               .getQlHelmConnectorInputBuilder(
                                   getQlGCSPlatformInputBuilder().bucketName(RequestField.absent()).build())
                               .build())
            .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Bucket name is not specified for GCS hosting platform");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateHelmGCSConnectorWithoutConnectorType() {
    QLUpdateConnectorInput input =
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(null)
            .helmConnector(Utility.getQlHelmConnectorInputBuilder(getQlGCSPlatformInputBuilder().build()).build())
            .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Invalid connector type provided");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateHelmGCSConnectorWithoutConnectorID() {
    QLUpdateConnectorInput input =
        QLUpdateConnectorInput.builder()
            .connectorId(null)
            .connectorType(QLConnectorType.GCS_HELM_REPO)
            .helmConnector(Utility.getQlHelmConnectorInputBuilder(getQlGCSPlatformInputBuilder().build()).build())
            .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Connector ID is not provided");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateHelmAmazonS3Connector() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withAccountId(ACCOUNT_ID)
                                   .withCategory(SettingAttribute.SettingCategory.HELM_REPO)
                                   .withValue(AmazonS3HelmRepoConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withCategory(SettingAttribute.SettingCategory.HELM_REPO)
                 .withValue(AmazonS3HelmRepoConfig.builder().accountId(ACCOUNT_ID).build())
                 .build())
        .when(settingsService)
        .updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);

    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    doReturn(QLAmazonS3HelmRepoConnector.builder()).when(connectorsController).getConnectorBuilder(any());
    doReturn(QLAmazonS3HelmRepoConnector.builder()).when(connectorsController).populateConnector(any(), any());
    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);
    doReturn(setting).when(settingsService).getByAccountAndId(ACCOUNT_ID, "AWS");

    QLUpdateConnectorPayload payload = dataFetcher.mutateAndFetch(
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.AMAZON_S3_HELM_REPO)
            .helmConnector(getQlHelmConnectorInputBuilder(
                getQlAmazonS3PlatformInputBuilder().bucketName(RequestField.ofNullable("newBucketName")).build())
                               .build())
            .build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).getByAccount(ACCOUNT_ID, CONNECTOR_ID);
    verify(settingsService, times(1)).updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    assertThat(payload.getConnector()).isNotNull();
    assertThat(payload.getConnector()).isInstanceOf(QLAmazonS3HelmRepoConnector.class);
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateHelmAmazonS3ConnectorWithoutProvider() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.HELM_REPO)
                                   .withValue(AmazonS3HelmRepoConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    QLUpdateConnectorInput input =
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.AMAZON_S3_HELM_REPO)
            .helmConnector(getQlHelmConnectorInputBuilder(
                getQlAmazonS3PlatformInputBuilder().awsCloudProvider(RequestField.absent()).build())
                               .build())
            .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("AWS Cloud provider is not specified for Amazon S3 hosting platform");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateHelmAmazonS3ConnectorWithoutBucketName() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withAccountId(ACCOUNT_ID)
                                   .withCategory(SettingAttribute.SettingCategory.HELM_REPO)
                                   .withValue(AmazonS3HelmRepoConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);
    doReturn(setting).when(settingsService).getByAccountAndId(ACCOUNT_ID, "AWS");

    QLUpdateConnectorInput input =
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.AMAZON_S3_HELM_REPO)
            .helmConnector(getQlHelmConnectorInputBuilder(
                getQlAmazonS3PlatformInputBuilder().bucketName(RequestField.absent()).build())
                               .build())
            .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Bucket name is not specified for Amazon S3 hosting platform");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateHelmAmazonS3ConnectorWithoutConnectorType() {
    QLUpdateConnectorInput input =
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(null)
            .helmConnector(getQlHelmConnectorInputBuilder(getQlAmazonS3PlatformInputBuilder().build()).build())
            .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Invalid connector type provided");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void updateHelmAmazonS3ConnectorWithoutConnectorID() {
    QLUpdateConnectorInput input =
        QLUpdateConnectorInput.builder()
            .connectorId(null)
            .connectorType(QLConnectorType.AMAZON_S3_HELM_REPO)
            .helmConnector(getQlHelmConnectorInputBuilder(getQlAmazonS3PlatformInputBuilder().build()).build())
            .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Connector ID is not provided");
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void updateHelmGCSConnectorThrowsException() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withAccountId(ACCOUNT_ID)
                                   .withCategory(SettingAttribute.SettingCategory.HELM_REPO)
                                   .withValue(GCSHelmRepoConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    verify(settingsService, never()).saveWithPruning(isA(SettingAttribute.class), isA(String.class), isA(String.class));

    doThrow(new ConstraintViolationException(new HashSet<>()))
        .when(settingsService)
        .updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);

    verify(settingServiceHelper, never())
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, PASSWORD);
    doReturn(setting).when(settingsService).getByAccountAndId(ACCOUNT_ID, "GCP");

    dataFetcher.mutateAndFetch(
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.GCS_HELM_REPO)
            .helmConnector(
                Utility
                    .getQlHelmConnectorInputBuilder(
                        getQlGCSPlatformInputBuilder().bucketName(RequestField.ofNullable("newBucketName")).build())
                    .build())
            .build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());
  }
}
