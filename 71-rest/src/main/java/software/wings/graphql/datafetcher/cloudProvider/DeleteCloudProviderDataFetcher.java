package software.wings.graphql.datafetcher.cloudProvider;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.SettingAttribute.SettingCategory.CLOUD_PROVIDER;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.cloudProvider.QLDeleteCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLDeleteCloudProviderPayload;
import software.wings.graphql.schema.type.QLCloudProviderType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SettingsService;

@Slf4j
public class DeleteCloudProviderDataFetcher
    extends BaseMutatorDataFetcher<QLDeleteCloudProviderInput, QLDeleteCloudProviderPayload> {
  @Inject private SettingsService settingsService;

  public DeleteCloudProviderDataFetcher() {
    super(QLDeleteCloudProviderInput.class, QLDeleteCloudProviderPayload.class);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLDeleteCloudProviderPayload mutateAndFetch(
      QLDeleteCloudProviderInput input, MutationContext mutationContext) {
    String cloudProviderId = input.getCloudProviderId();
    String accountId = mutationContext.getAccountId();

    if (isBlank(cloudProviderId)) {
      throw new InvalidRequestException("The cloudProviderId cannot be null");
    }

    if (input.getCloudProviderType() == null) {
      throw new InvalidRequestException("Invalid cloudProviderType provided in the request");
    }

    SettingAttribute settingAttribute = settingsService.getByAccount(accountId, cloudProviderId);

    if (settingAttribute == null || settingAttribute.getValue() == null
        || CLOUD_PROVIDER != settingAttribute.getCategory()
        || QLCloudProviderType.valueOf(settingAttribute.getValue().getType()) == null) {
      throw new InvalidRequestException(
          String.format("No cloud provider exists with the cloudProviderId %s", cloudProviderId));
    }

    settingsService.delete(null, cloudProviderId);

    return QLDeleteCloudProviderPayload.builder().clientMutationId(input.getClientMutationId()).build();
  }
}
