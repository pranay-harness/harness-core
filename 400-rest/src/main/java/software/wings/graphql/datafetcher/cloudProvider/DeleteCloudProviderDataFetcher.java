package software.wings.graphql.datafetcher.cloudProvider;

import static software.wings.beans.SettingAttribute.SettingCategory.CLOUD_PROVIDER;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CLOUD_PROVIDERS;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.cloudProvider.QLDeleteCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLDeleteCloudProviderPayload;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(Module._380_CG_GRAPHQL)
public class DeleteCloudProviderDataFetcher
    extends BaseMutatorDataFetcher<QLDeleteCloudProviderInput, QLDeleteCloudProviderPayload> {
  @Inject private SettingsService settingsService;

  @Inject
  public DeleteCloudProviderDataFetcher() {
    super(QLDeleteCloudProviderInput.class, QLDeleteCloudProviderPayload.class);
  }

  @Override
  @AuthRule(permissionType = MANAGE_CLOUD_PROVIDERS)
  protected QLDeleteCloudProviderPayload mutateAndFetch(
      QLDeleteCloudProviderInput input, MutationContext mutationContext) {
    String cloudProviderId = input.getCloudProviderId();
    String accountId = mutationContext.getAccountId();

    if (isBlank(cloudProviderId)) {
      throw new InvalidRequestException("The cloudProviderId cannot be null");
    }

    SettingAttribute settingAttribute = settingsService.getByAccount(accountId, cloudProviderId);
    if (validForDeletion(settingAttribute)) {
      settingsService.delete(null, cloudProviderId);
    }

    return QLDeleteCloudProviderPayload.builder().clientMutationId(input.getClientMutationId()).build();
  }

  private boolean validForDeletion(SettingAttribute settingAttribute) {
    return settingAttribute != null && settingAttribute.getValue() != null
        && CLOUD_PROVIDER == settingAttribute.getCategory();
  }
}
