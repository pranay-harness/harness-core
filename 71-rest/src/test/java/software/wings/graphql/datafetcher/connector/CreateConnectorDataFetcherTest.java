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
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.connector.input.QLCreateConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLGitConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLGitConnectorInput.QLGitConnectorInputBuilder;
import software.wings.graphql.schema.mutation.connector.payload.QLCreateConnectorPayload;
import software.wings.graphql.schema.type.QLConnectorType;
import software.wings.graphql.schema.type.connector.QLGitConnector;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.sql.SQLException;

public class CreateConnectorDataFetcherTest {
  @Mock private SettingsService settingsService;
  @Mock private SettingServiceHelper settingServiceHelper;
  @Mock private GitDataFetcherHelper gitDataFetcherHelper;
  @Mock private ConnectorsController connectorsController;
  @Mock private SecretManager secretManager;

  @InjectMocks private CreateConnectorDataFetcher dataFetcher = new CreateConnectorDataFetcher();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void createGitConnector() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                                   .withValue(GitConfig.builder().accountId("ACCOUNT_ID").build())
                                   .build();

    doReturn(setting).when(gitDataFetcherHelper).toSettingAttribute(isA(QLGitConnectorInput.class), isA(String.class));

    doReturn(setting)
        .when(settingsService)
        .saveWithPruning(isA(SettingAttribute.class), isA(String.class), isA(String.class));

    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    doReturn(QLGitConnector.builder()).when(connectorsController).getConnectorBuilder(any());
    doReturn(QLGitConnector.builder()).when(connectorsController).populateConnector(any(), any());
    doReturn(new EncryptedData()).when(secretManager).getSecretById("ACCOUNT_ID", "PASSWORD");

    QLCreateConnectorPayload payload = dataFetcher.mutateAndFetch(
        QLCreateConnectorInput.builder()
            .connectorType(QLConnectorType.GIT)
            .gitConnector(getQlGitConnectorInputBuilder().passwordSecretId(RequestField.ofNullable("PASSWORD")).build())
            .build(),
        MutationContext.builder().accountId("ACCOUNT_ID").build());

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
    QLCreateConnectorInput input = QLCreateConnectorInput.builder()
                                       .connectorType(QLConnectorType.GIT)
                                       .gitConnector(getQlGitConnectorInputBuilder()
                                                         .passwordSecretId(RequestField.ofNullable("PASSWORD"))
                                                         .sshSettingId(RequestField.ofNullable("SSH"))
                                                         .build())
                                       .build();
    MutationContext mutationContext = MutationContext.builder().accountId("ACCOUNT_ID").build();
    doReturn(new SettingAttribute()).when(settingsService).getByAccount("ACCOUNT_ID", "PASSWORD");
    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Just one secretId should be specified");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void createGitConnectorNotSpecifyingSecrets() {
    QLCreateConnectorInput input = QLCreateConnectorInput.builder()
                                       .connectorType(QLConnectorType.GIT)
                                       .gitConnector(getQlGitConnectorInputBuilder().build())
                                       .build();
    MutationContext mutationContext = MutationContext.builder().accountId("ACCOUNT_ID").build();
    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("No secretId provided with the request for connector");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void createGitConnectorWithPasswordNotSpecifyingUsername() {
    QLGitConnectorInputBuilder gitConnectorInputBuilder = getQlGitConnectorInputBuilder();
    gitConnectorInputBuilder.userName(RequestField.absent()).passwordSecretId(RequestField.ofNullable("PASSWORD"));
    QLCreateConnectorInput input = QLCreateConnectorInput.builder()
                                       .connectorType(QLConnectorType.GIT)
                                       .gitConnector(gitConnectorInputBuilder.build())
                                       .build();
    MutationContext mutationContext = MutationContext.builder().accountId("ACCOUNT_ID").build();
    doReturn(new SettingAttribute()).when(settingsService).getByAccount("ACCOUNT_ID", "PASSWORD");

    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("userName should be specified");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void createGitConnectorWitNonExistentSecretId() {
    QLGitConnectorInputBuilder gitConnectorInputBuilder = getQlGitConnectorInputBuilder();
    gitConnectorInputBuilder.userName(RequestField.ofNullable("username"))
        .passwordSecretId(RequestField.ofNullable("password"));
    QLCreateConnectorInput input = QLCreateConnectorInput.builder()
                                       .connectorType(QLConnectorType.GIT)
                                       .gitConnector(gitConnectorInputBuilder.build())
                                       .build();
    MutationContext mutationContext = MutationContext.builder().accountId("ACCOUNT_ID").build();
    doReturn(null).when(secretManager).getSecretById("ACCOUNT_ID", "password");
    assertThatThrownBy(() -> dataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Secret does not exist");
  }

  private QLGitConnectorInputBuilder getQlGitConnectorInputBuilder() {
    return QLGitConnectorInput.builder()
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
