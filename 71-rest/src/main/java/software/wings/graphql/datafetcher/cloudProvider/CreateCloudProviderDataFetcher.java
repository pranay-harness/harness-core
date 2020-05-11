package software.wings.graphql.datafetcher.cloudProvider;

import static software.wings.graphql.datafetcher.cloudProvider.CloudProviderController.checkIfInputIsNotPresent;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.GcpConfig;
import software.wings.beans.GcpConfig.GcpConfigBuilder;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfConfig.PcfConfigBuilder;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.SpotInstConfig.SpotInstConfigBuilder;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLCreateCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLCreateCloudProviderPayload;
import software.wings.graphql.schema.mutation.cloudProvider.QLCreateCloudProviderPayload.QLCreateCloudProviderPayloadBuilder;
import software.wings.graphql.schema.mutation.cloudProvider.QLGcpCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLPcfCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLSpotInstCloudProviderInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.service.intfc.SettingsService;

@Slf4j
public class CreateCloudProviderDataFetcher
    extends BaseMutatorDataFetcher<QLCreateCloudProviderInput, QLCreateCloudProviderPayload> {
  @Inject private SettingsService settingsService;
  @Inject private UsageScopeController usageScopeController;
  @Inject private SettingServiceHelper settingServiceHelper;

  public CreateCloudProviderDataFetcher() {
    super(QLCreateCloudProviderInput.class, QLCreateCloudProviderPayload.class);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLCreateCloudProviderPayload mutateAndFetch(
      QLCreateCloudProviderInput input, MutationContext mutationContext) {
    QLCreateCloudProviderPayloadBuilder builder =
        QLCreateCloudProviderPayload.builder().clientMutationId(input.getClientMutationId());

    if (input.getCloudProviderType() == null) {
      throw new InvalidRequestException("Invalid cloudProviderType provided in the request");
    }

    SettingAttribute settingAttribute;
    switch (input.getCloudProviderType()) {
      case PCF:
        checkIfInputIsNotPresent(input.getCloudProviderType(), input.getPcfCloudProvider());
        settingAttribute = toSettingAttribute(input.getPcfCloudProvider(), mutationContext.getAccountId());
        break;
      case SPOT_INST:
        checkIfInputIsNotPresent(input.getCloudProviderType(), input.getSpotInstCloudProvider());
        settingAttribute = toSettingAttribute(input.getSpotInstCloudProvider(), mutationContext.getAccountId());
        break;
      case GCP:
        checkIfInputIsNotPresent(input.getCloudProviderType(), input.getGcpCloudProvider());
        settingAttribute = toSettingAttribute(input.getGcpCloudProvider(), mutationContext.getAccountId());
        break;
      default:
        throw new InvalidRequestException("Invalid cloud provider Type");
    }

    settingAttribute =
        settingsService.saveWithPruning(settingAttribute, Application.GLOBAL_APP_ID, mutationContext.getAccountId());
    settingServiceHelper.updateSettingAttributeBeforeResponse(settingAttribute, false);
    return builder.cloudProvider(CloudProviderController.populateCloudProvider(settingAttribute).build()).build();
  }

  private SettingAttribute toSettingAttribute(QLPcfCloudProviderInput input, String accountId) {
    PcfConfigBuilder pcfConfigBuilder = PcfConfig.builder().accountId(accountId);

    if (input.getEndpointUrl().isPresent()) {
      input.getEndpointUrl().getValue().ifPresent(pcfConfigBuilder::endpointUrl);
    }
    if (input.getUserName().isPresent()) {
      input.getUserName().getValue().ifPresent(pcfConfigBuilder::username);
    }
    if (input.getPasswordSecretId().isPresent()) {
      input.getPasswordSecretId().getValue().ifPresent(pcfConfigBuilder::encryptedPassword);
    }

    SettingAttribute.Builder settingAttributeBuilder = SettingAttribute.Builder.aSettingAttribute()
                                                           .withValue(pcfConfigBuilder.build())
                                                           .withAccountId(accountId)
                                                           .withCategory(SettingAttribute.SettingCategory.SETTING);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttributeBuilder::withName);
    }

    if (input.getUsageScope().isPresent()) {
      settingAttributeBuilder.withUsageRestrictions(
          usageScopeController.populateUsageRestrictions(input.getUsageScope().getValue().orElse(null), accountId));
    }

    return settingAttributeBuilder.build();
  }

  private SettingAttribute toSettingAttribute(QLSpotInstCloudProviderInput input, String accountId) {
    SpotInstConfigBuilder configBuilder = SpotInstConfig.builder().accountId(accountId);

    if (input.getAccountId().isPresent()) {
      input.getAccountId().getValue().ifPresent(configBuilder::spotInstAccountId);
    }
    if (input.getTokenSecretId().isPresent()) {
      input.getTokenSecretId().getValue().ifPresent(configBuilder::encryptedSpotInstToken);
    }

    SettingAttribute.Builder settingAttributeBuilder = SettingAttribute.Builder.aSettingAttribute()
                                                           .withValue(configBuilder.build())
                                                           .withAccountId(accountId)
                                                           .withCategory(SettingAttribute.SettingCategory.SETTING);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttributeBuilder::withName);
    }

    if (input.getUsageScope().isPresent()) {
      settingAttributeBuilder.withUsageRestrictions(
          usageScopeController.populateUsageRestrictions(input.getUsageScope().getValue().orElse(null), accountId));
    }

    return settingAttributeBuilder.build();
  }

  private SettingAttribute toSettingAttribute(QLGcpCloudProviderInput input, String accountId) {
    GcpConfigBuilder configBuilder = GcpConfig.builder().accountId(accountId);

    if (input.getServiceAccountKeySecretId().isPresent()) {
      input.getServiceAccountKeySecretId().getValue().ifPresent(configBuilder::encryptedServiceAccountKeyFileContent);
    }

    SettingAttribute.Builder settingAttributeBuilder = SettingAttribute.Builder.aSettingAttribute()
                                                           .withValue(configBuilder.build())
                                                           .withAccountId(accountId)
                                                           .withCategory(SettingAttribute.SettingCategory.SETTING);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttributeBuilder::withName);
    }

    if (input.getUsageScope().isPresent()) {
      settingAttributeBuilder.withUsageRestrictions(
          usageScopeController.populateUsageRestrictions(input.getUsageScope().getValue().orElse(null), accountId));
    }

    return settingAttributeBuilder.build();
  }
}
