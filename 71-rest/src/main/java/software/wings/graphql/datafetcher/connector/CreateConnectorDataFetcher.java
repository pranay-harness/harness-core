package software.wings.graphql.datafetcher.connector;

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
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.ConstraintViolationHandlerUtils;

import javax.validation.ConstraintViolationException;

@Slf4j
public class CreateConnectorDataFetcher
    extends BaseMutatorDataFetcher<QLCreateConnectorInput, QLCreateConnectorPayload> {
  @Inject private SettingsService settingsService;
  @Inject private SettingServiceHelper settingServiceHelper;
  @Inject private GitDataFetcherHelper gitDataFetcherHelper;
  @Inject private ConnectorsController connectorsController;
  @Inject private SecretManager secretManager;

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
      connectorsController.checkIfInputIsNotPresent(input.getConnectorType(), input.getGitConnector());
      checkSecrets(input.getGitConnector(), mutationContext.getAccountId());

      settingAttribute =
          gitDataFetcherHelper.toSettingAttribute(input.getGitConnector(), mutationContext.getAccountId());
    } else {
      throw new InvalidRequestException("Invalid connector Type");
    }

    try {
      settingAttribute =
          settingsService.saveWithPruning(settingAttribute, Application.GLOBAL_APP_ID, mutationContext.getAccountId());
    } catch (ConstraintViolationException exception) {
      String errorMessages = String.join(", ", ConstraintViolationHandlerUtils.getErrorMessages(exception));
      throw new InvalidRequestException(errorMessages, exception);
    }

    settingServiceHelper.updateSettingAttributeBeforeResponse(settingAttribute, false);

    QLConnectorBuilder qlGitConnectorBuilder = connectorsController.getConnectorBuilder(settingAttribute);
    return builder.connector(connectorsController.populateConnector(settingAttribute, qlGitConnectorBuilder).build())
        .build();
  }

  private void checkSecrets(QLGitConnectorInput gitConnectorInput, String accountId) {
    boolean passwordSecretIsPresent = false;
    boolean sshSettingIdIsPresent = false;
    if (gitConnectorInput.getPasswordSecretId().isPresent()
        && gitConnectorInput.getPasswordSecretId().getValue().isPresent()) {
      if (!gitConnectorInput.getUserName().isPresent() || !gitConnectorInput.getUserName().getValue().isPresent()) {
        throw new InvalidRequestException("userName should be specified");
      }
      passwordSecretIsPresent = true;
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
    if (passwordSecretIsPresent) {
      checkIfSecretExists(accountId, gitConnectorInput.getPasswordSecretId().getValue().get());
    }
    if (sshSettingIdIsPresent) {
      checkIfSshSettingExists(accountId, gitConnectorInput.getSshSettingId().getValue().get());
    }
  }

  private void checkIfSecretExists(String accountId, String secretId) {
    if (secretManager.getSecretById(accountId, secretId) == null) {
      throw new InvalidRequestException("Secret does not exist");
    }
  }

  private void checkIfSshSettingExists(String accountId, String secretId) {
    if (settingsService.getByAccount(accountId, secretId) == null) {
      throw new InvalidRequestException("Secret does not exist");
    }
  }
}
