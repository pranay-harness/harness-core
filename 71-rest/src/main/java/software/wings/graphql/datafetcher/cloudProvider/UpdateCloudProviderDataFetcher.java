package software.wings.graphql.datafetcher.cloudProvider;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.SettingAttribute.SettingCategory.CLOUD_PROVIDER;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.PcfConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLPcfCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateCloudProviderPayload;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateCloudProviderPayload.QLUpdateCloudProviderPayloadBuilder;
import software.wings.graphql.schema.type.QLCloudProviderType;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

@Slf4j
public class UpdateCloudProviderDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateCloudProviderInput, QLUpdateCloudProviderPayload> {
  @Inject private SettingsService settingsService;
  @Inject private UsageScopeController usageScopeController;
  @Inject private SettingServiceHelper settingServiceHelper;

  public UpdateCloudProviderDataFetcher() {
    super(QLUpdateCloudProviderInput.class, QLUpdateCloudProviderPayload.class);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLUpdateCloudProviderPayload mutateAndFetch(
      QLUpdateCloudProviderInput input, MutationContext mutationContext) {
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

    QLUpdateCloudProviderPayloadBuilder builder =
        QLUpdateCloudProviderPayload.builder().clientMutationId(mutationContext.getAccountId());

    switch (input.getCloudProviderType()) {
      case PCF:
        updateSettingAttribute(settingAttribute, input.getPcfCloudProvider(), mutationContext.getAccountId());
        SettingAttribute updatedSettings =
            settingsService.updateWithSettingFields(settingAttribute, settingAttribute.getUuid(), GLOBAL_APP_ID);

        settingServiceHelper.updateEncryptedFieldsInResponse(updatedSettings, false);

        return builder.cloudProvider(CloudProviderController.populateCloudProvider(updatedSettings).build()).build();
      default:
        throw new InvalidRequestException("Invalid cloud provider type");
    }
  }

  private void updateSettingAttribute(
      SettingAttribute settingAttribute, QLPcfCloudProviderInput input, String accountId) {
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();

    if (input.getEndpointUrl().isPresent()) {
      input.getEndpointUrl().getValue().ifPresent(pcfConfig::setEndpointUrl);
    }
    if (input.getUsername().isPresent()) {
      input.getUsername().getValue().ifPresent(pcfConfig::setUsername);
    }
    pcfConfig.setPassword(SecretManager.ENCRYPTED_FIELD_MASK.toCharArray());
    if (input.getPassword().isPresent()) {
      input.getPassword().getValue().map(String::toCharArray).ifPresent(pcfConfig::setPassword);
    }
    settingAttribute.setValue(pcfConfig);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttribute::setName);
    }

    if (input.getUsageScope().isPresent()) {
      QLUsageScope usageScope = input.getUsageScope().getValue().orElse(null);
      settingAttribute.setUsageRestrictions(usageScopeController.populateUsageRestrictions(usageScope, accountId));
    }
  }
}
