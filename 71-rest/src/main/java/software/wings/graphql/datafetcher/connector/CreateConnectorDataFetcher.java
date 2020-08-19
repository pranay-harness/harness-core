package software.wings.graphql.datafetcher.connector;

import static software.wings.graphql.datafetcher.connector.ConnectorsController.checkIfInputIsNotPresent;
import static software.wings.graphql.schema.type.QLConnectorType.GIT;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.connector.input.QLCreateConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLGitConnectorInput;
import software.wings.graphql.schema.mutation.connector.payload.QLCreateConnectorPayload;
import software.wings.graphql.schema.mutation.connector.payload.QLCreateConnectorPayload.QLCreateConnectorPayloadBuilder;
import software.wings.graphql.schema.type.connector.QLConnectorBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.service.intfc.SettingsService;

@Slf4j
public class CreateConnectorDataFetcher
    extends BaseMutatorDataFetcher<QLCreateConnectorInput, QLCreateConnectorPayload> {
  @Inject private SettingsService settingsService;
  @Inject private SettingServiceHelper settingServiceHelper;
  @Inject private GitDataFetcherHelper gitDataFetcherHelper;

  public CreateConnectorDataFetcher() {
    super(QLCreateConnectorInput.class, QLCreateConnectorPayload.class);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.MANAGE_CONNECTORS)
  protected QLCreateConnectorPayload mutateAndFetch(QLCreateConnectorInput input, MutationContext mutationContext) {
    QLCreateConnectorPayloadBuilder builder =
        QLCreateConnectorPayload.builder().clientMutationId(input.getClientMutationId());

    if (input.getConnectorType() == null) {
      throw new InvalidRequestException("Invalid connector provided in the request");
    }

    SettingAttribute settingAttribute;
    if (GIT == input.getConnectorType()) {
      checkIfInputIsNotPresent(input.getConnectorType(), input.getGitConnector());
      checkSecrets(input.getGitConnector());

      settingAttribute =
          gitDataFetcherHelper.toSettingAttribute(input.getGitConnector(), mutationContext.getAccountId());
    } else {
      throw new InvalidRequestException("Invalid connector Type");
    }

    settingAttribute =
        settingsService.saveWithPruning(settingAttribute, Application.GLOBAL_APP_ID, mutationContext.getAccountId());
    settingServiceHelper.updateSettingAttributeBeforeResponse(settingAttribute, false);

    QLConnectorBuilder qlGitConnectorBuilder = ConnectorsController.getConnectorBuilder(settingAttribute);
    return builder.connector(ConnectorsController.populateConnector(settingAttribute, qlGitConnectorBuilder).build())
        .build();
  }

  private void checkSecrets(QLGitConnectorInput gitConnectorInput) {
    boolean passwordSecretIsPresent = false;
    boolean sshSettingIdIsPresent = false;
    if (gitConnectorInput.getPasswordSecretId().isPresent()) {
      passwordSecretIsPresent = gitConnectorInput.getPasswordSecretId().getValue().isPresent();
    }
    if (gitConnectorInput.getSshSettingId().isPresent()) {
      sshSettingIdIsPresent = gitConnectorInput.getSshSettingId().getValue().isPresent();
    }
    if (!passwordSecretIsPresent && !sshSettingIdIsPresent) {
      throw new InvalidRequestException("No secretId provided with the request for connector");
    }
    if (passwordSecretIsPresent && sshSettingIdIsPresent) {
      throw new InvalidRequestException("Just one secretId should be specified");
    }
  }
}
