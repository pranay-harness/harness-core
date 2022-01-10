/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.connector;

import static software.wings.beans.SettingAttribute.SettingCategory.CONNECTOR;
import static software.wings.beans.SettingAttribute.SettingCategory.HELM_REPO;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.connector.input.QLDeleteConnectorInput;
import software.wings.graphql.schema.mutation.connector.payload.QLDeleteConnectorPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
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

    validateForDeletion(settingAttribute, connectorId);
    settingsService.delete(null, connectorId);

    return QLDeleteConnectorPayload.builder().clientMutationId(input.getClientMutationId()).build();
  }

  private void validateForDeletion(SettingAttribute settingAttribute, String connectorId) {
    if (settingAttribute == null || settingAttribute.getValue() == null
        || (CONNECTOR != settingAttribute.getCategory() && HELM_REPO != settingAttribute.getCategory())) {
      throw new InvalidRequestException(String.format("Invalid connectorId: %s", connectorId));
    }
  }
}
