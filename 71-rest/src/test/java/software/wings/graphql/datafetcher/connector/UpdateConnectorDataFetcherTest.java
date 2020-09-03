package software.wings.graphql.datafetcher.connector;

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
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.connector.input.QLUpdateConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLUpdateGitConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLUpdateGitConnectorInput.QLUpdateGitConnectorInputBuilder;
import software.wings.graphql.schema.mutation.connector.payload.QLUpdateConnectorPayload;
import software.wings.graphql.schema.type.QLConnectorType;
import software.wings.graphql.schema.type.connector.QLGitConnector;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.sql.SQLException;

public class UpdateConnectorDataFetcherTest extends AbstractDataFetcherTest {
  private static final String CONNECTOR_ID = "C-ID";
  private static final String ACCOUNT_ID = "ACCOUNT-ID";

  @Mock private GitDataFetcherHelper gitDataFetcherHelper;
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
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void updateGit() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withAccountId(ACCOUNT_ID)
                                   .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                                   .withValue(GitConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    doNothing()
        .when(gitDataFetcherHelper)
        .updateSettingAttribute(isA(SettingAttribute.class), isA(QLUpdateGitConnectorInput.class));

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
    doReturn(new EncryptedData()).when(secretManager).getSecretById(ACCOUNT_ID, "PASSWORD");

    QLUpdateConnectorPayload payload = dataFetcher.mutateAndFetch(
        QLUpdateConnectorInput.builder()
            .connectorId(CONNECTOR_ID)
            .connectorType(QLConnectorType.GIT)
            .gitConnector(
                getQlUpdateGitConnectorInputBuilder().passwordSecretId(RequestField.ofNullable("PASSWORD")).build())
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

    QLUpdateConnectorInput input = QLUpdateConnectorInput.builder()
                                       .connectorId(CONNECTOR_ID)
                                       .connectorType(QLConnectorType.GIT)
                                       .gitConnector(getQlUpdateGitConnectorInputBuilder()
                                                         .passwordSecretId(RequestField.ofNullable("PASSWORD"))
                                                         .sshSettingId(RequestField.ofNullable("SSH"))
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
            .gitConnector(getQlUpdateGitConnectorInputBuilder().sshSettingId(RequestField.ofNullable("SSH")).build())
            .build();
    MutationContext context = MutationContext.builder().accountId(ACCOUNT_ID).build();

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, context))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Secret does not exit");
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
            .passwordSecretId(RequestField.ofNullable("PASSWORD"));

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
        .hasMessage("No connector exists with the connectorId C-ID");
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

  private QLUpdateGitConnectorInputBuilder getQlUpdateGitConnectorInputBuilder() {
    return QLUpdateGitConnectorInput.builder()
        .name(RequestField.ofNullable("NAME"))
        .URL(RequestField.ofNullable("URL"))
        .userName(RequestField.ofNullable("USER"))
        .branch(RequestField.absent())
        .passwordSecretId(RequestField.absent())
        .sshSettingId(RequestField.absent())
        .generateWebhookUrl(RequestField.absent())
        .customCommitDetails(RequestField.absent());
  }
}
