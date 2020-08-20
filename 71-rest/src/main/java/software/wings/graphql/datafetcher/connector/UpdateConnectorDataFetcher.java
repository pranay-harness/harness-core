package software.wings.graphql.datafetcher.connector;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.SettingAttribute.SettingCategory.CONNECTOR;
import static software.wings.graphql.datafetcher.connector.ConnectorsController.checkIfInputIsNotPresent;
import static software.wings.graphql.schema.type.QLConnectorType.GIT;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CONNECTORS;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.connector.input.QLUpdateConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLUpdateGitConnectorInput;
import software.wings.graphql.schema.mutation.connector.payload.QLUpdateConnectorPayload;
import software.wings.graphql.schema.mutation.connector.payload.QLUpdateConnectorPayload.QLUpdateConnectorPayloadBuilder;
import software.wings.graphql.schema.type.QLConnectorType;
import software.wings.graphql.schema.type.connector.QLConnectorBuilder;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.service.intfc.SettingsService;

@Slf4j
public class UpdateConnectorDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateConnectorInput, QLUpdateConnectorPayload> {
  @Inject private SettingsService settingsService;
  @Inject private SettingServiceHelper settingServiceHelper;
  @Inject private GitDataFetcherHelper gitDataFetcherHelper;

  public UpdateConnectorDataFetcher() {
    super(QLUpdateConnectorInput.class, QLUpdateConnectorPayload.class);
  }

  @Override
  @AuthRule(permissionType = MANAGE_CONNECTORS)
  protected QLUpdateConnectorPayload mutateAndFetch(QLUpdateConnectorInput input, MutationContext mutationContext) {
    String connectorId = input.getConnectorId();
    String accountId = mutationContext.getAccountId();

    if (isBlank(connectorId)) {
      throw new InvalidRequestException("The connectorId cannot be null");
    }

    if (input.getConnectorType() == null) {
      throw new InvalidRequestException("Invalid connectorType provided in the request");
    }

    SettingAttribute settingAttribute = settingsService.getByAccount(accountId, connectorId);
    if (settingAttribute == null || settingAttribute.getValue() == null
        || CONNECTOR != settingAttribute.getCategory()) {
      throw new InvalidRequestException(String.format("No connector exists with the connectorId %s", connectorId));
    }
    checkIfConnectorTypeMatchesTheInputType(settingAttribute.getValue().getType(), input.getConnectorType());
    QLUpdateConnectorPayloadBuilder builder =
        QLUpdateConnectorPayload.builder().clientMutationId(input.getClientMutationId());

    if (GIT == input.getConnectorType()) {
      checkIfInputIsNotPresent(input.getConnectorType(), input.getGitConnector());
      checkSecrets(input.getGitConnector(), settingAttribute);
      gitDataFetcherHelper.updateSettingAttribute(settingAttribute, input.getGitConnector());
    } else {
      throw new InvalidRequestException("Invalid connector Type");
    }

    settingAttribute =
        settingsService.updateWithSettingFields(settingAttribute, settingAttribute.getUuid(), GLOBAL_APP_ID);
    settingServiceHelper.updateSettingAttributeBeforeResponse(settingAttribute, false);

    QLConnectorBuilder qlGitConnectorBuilder = ConnectorsController.getConnectorBuilder(settingAttribute);
    return builder.connector(ConnectorsController.populateConnector(settingAttribute, qlGitConnectorBuilder).build())
        .build();
  }

  private void checkIfConnectorTypeMatchesTheInputType(String settingVariableType, QLConnectorType connectorType) {
    if (!settingVariableType.equals(connectorType.toString())) {
      throw new InvalidRequestException(
          String.format("The existing connector is of type %s and the update operation inputs a connector of type %s",
              settingVariableType, connectorType));
    }
  }

  private void checkSecrets(QLUpdateGitConnectorInput gitConnectorInput, SettingAttribute settingAttribute) {
    boolean passwordSecretIsPresent = false;
    boolean sshSettingIdIsPresent = false;
    if (gitConnectorInput.getPasswordSecretId().isPresent()) {
      throwExceptionIfUsernameShoulBeSpecified(gitConnectorInput, settingAttribute);
      passwordSecretIsPresent = gitConnectorInput.getPasswordSecretId().getValue().isPresent();
    }
    if (gitConnectorInput.getSshSettingId().isPresent()) {
      sshSettingIdIsPresent = gitConnectorInput.getSshSettingId().getValue().isPresent();
    }
    if (passwordSecretIsPresent && sshSettingIdIsPresent) {
      throw new InvalidRequestException("Just one secretId should be specified");
    }
  }

  private void throwExceptionIfUsernameShoulBeSpecified(
      QLUpdateGitConnectorInput gitConnectorInput, SettingAttribute settingAttribute) {
    if (null == ((GitConfig) settingAttribute.getValue()).getUsername()) {
      if (!gitConnectorInput.getUserName().isPresent() || !gitConnectorInput.getUserName().getValue().isPresent()) {
        throw new InvalidRequestException("userName should be specified");
      }
    }
  }
}
