package software.wings.graphql.datafetcher.cloudProvider;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.SettingAttribute.SettingCategory.CLOUD_PROVIDER;
import static software.wings.graphql.datafetcher.cloudProvider.CloudProviderController.checkIfInputIsNotPresent;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateCloudProviderPayload;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateCloudProviderPayload.QLUpdateCloudProviderPayloadBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.service.intfc.SettingsService;

@Slf4j
public class UpdateCloudProviderDataFetcher
    extends BaseMutatorDataFetcher<QLUpdateCloudProviderInput, QLUpdateCloudProviderPayload> {
  @Inject private SettingsService settingsService;
  @Inject private SettingServiceHelper settingServiceHelper;
  @Inject private K8sDataFetcherHelper k8sDataFetcherHelper;
  @Inject private GcpDataFetcherHelper gcpDataFetcherHelper;
  @Inject private SpotInstDataFetcherHelper spotInstDataFetcherHelper;
  @Inject private PcfDataFetcherHelper pcfDataFetcherHelper;

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
        || CLOUD_PROVIDER != settingAttribute.getCategory()) {
      throw new InvalidRequestException(
          String.format("No cloud provider exists with the cloudProviderId %s", cloudProviderId));
    }

    QLUpdateCloudProviderPayloadBuilder builder =
        QLUpdateCloudProviderPayload.builder().clientMutationId(input.getClientMutationId());

    switch (input.getCloudProviderType()) {
      case PCF:
        checkIfInputIsNotPresent(input.getCloudProviderType(), input.getPcfCloudProvider());
        pcfDataFetcherHelper.updateSettingAttribute(
            settingAttribute, input.getPcfCloudProvider(), mutationContext.getAccountId());
        break;
      case SPOT_INST:
        checkIfInputIsNotPresent(input.getCloudProviderType(), input.getSpotInstCloudProvider());
        spotInstDataFetcherHelper.updateSettingAttribute(
            settingAttribute, input.getSpotInstCloudProvider(), mutationContext.getAccountId());
        break;
      case GCP:
        checkIfInputIsNotPresent(input.getCloudProviderType(), input.getGcpCloudProvider());
        gcpDataFetcherHelper.updateSettingAttribute(
            settingAttribute, input.getGcpCloudProvider(), mutationContext.getAccountId());
        break;
      case KUBERNETES_CLUSTER:
        checkIfInputIsNotPresent(input.getCloudProviderType(), input.getK8sCloudProvider());
        k8sDataFetcherHelper.updateSettingAttribute(
            settingAttribute, input.getK8sCloudProvider(), mutationContext.getAccountId());
        break;
      default:
        throw new InvalidRequestException("Invalid cloud provider type");
    }

    settingAttribute =
        settingsService.updateWithSettingFields(settingAttribute, settingAttribute.getUuid(), GLOBAL_APP_ID);
    settingServiceHelper.updateSettingAttributeBeforeResponse(settingAttribute, false);
    return builder.cloudProvider(CloudProviderController.populateCloudProvider(settingAttribute).build()).build();
  }
}
