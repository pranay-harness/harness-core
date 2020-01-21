package software.wings.graphql.datafetcher.cloudProvider;

import static software.wings.graphql.datafetcher.cloudProvider.CloudProviderController.populateCloudProvider;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLCloudProviderQueryParameters;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProvider;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class CloudProviderDataFetcher
    extends AbstractObjectDataFetcher<QLCloudProvider, QLCloudProviderQueryParameters> {
  @Inject private HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLCloudProvider fetch(QLCloudProviderQueryParameters qlQuery, String accountId) {
    SettingAttribute settingAttribute = persistence.get(SettingAttribute.class, qlQuery.getCloudProviderId());
    if (settingAttribute == null) {
      throw new InvalidRequestException("Cloud Provider does not exist", WingsException.USER);
    }

    if (!settingAttribute.getAccountId().equals(accountId)) {
      throw new InvalidRequestException("Cloud Provider does not exist", WingsException.USER);
    }

    return populateCloudProvider(settingAttribute).build();
  }
}
