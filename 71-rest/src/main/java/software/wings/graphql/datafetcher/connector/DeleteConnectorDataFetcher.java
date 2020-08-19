package software.wings.graphql.datafetcher.connector;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.SettingAttribute.SettingCategory.CONNECTOR;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.connector.input.QLDeleteConnectorInput;
import software.wings.graphql.schema.mutation.connector.payload.QLDeleteConnectorPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SettingsService;

@Slf4j
public class DeleteConnectorDataFetcher
    extends BaseMutatorDataFetcher<QLDeleteConnectorInput, QLDeleteConnectorPayload> {
  @Inject private SettingsService settingsService;

  public DeleteConnectorDataFetcher() {
    super(QLDeleteConnectorInput.class, QLDeleteConnectorPayload.class);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.MANAGE_CONNECTORS)
  protected QLDeleteConnectorPayload mutateAndFetch(QLDeleteConnectorInput input, MutationContext mutationContext) {
    String connectorId = input.getConnectorId();
    String accountId = mutationContext.getAccountId();

    if (isBlank(connectorId)) {
      throw new InvalidRequestException("The connectorId cannot be null");
    }

    SettingAttribute settingAttribute = settingsService.getByAccount(accountId, connectorId);

    if (validForDeletion(settingAttribute)) {
      settingsService.delete(null, connectorId);
    }

    return QLDeleteConnectorPayload.builder().clientMutationId(input.getClientMutationId()).build();
  }

  private boolean validForDeletion(SettingAttribute settingAttribute) {
    return settingAttribute != null && settingAttribute.getValue() != null
        && CONNECTOR == settingAttribute.getCategory();
  }
}
