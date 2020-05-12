package software.wings.graphql.datafetcher.cloudProvider;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.SettingAttribute.SettingCategory.CLOUD_PROVIDER;
import static software.wings.graphql.datafetcher.cloudProvider.CloudProviderController.checkIfInputIsNotPresent;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterAuthType;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.PcfConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SpotInstConfig;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLGcpCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLPcfCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLSpotInstCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateCloudProviderPayload;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateCloudProviderPayload.QLUpdateCloudProviderPayloadBuilder;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLK8sCloudProviderInput;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.service.intfc.SettingsService;

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
        || CLOUD_PROVIDER != settingAttribute.getCategory()) {
      throw new InvalidRequestException(
          String.format("No cloud provider exists with the cloudProviderId %s", cloudProviderId));
    }

    QLUpdateCloudProviderPayloadBuilder builder =
        QLUpdateCloudProviderPayload.builder().clientMutationId(input.getClientMutationId());

    switch (input.getCloudProviderType()) {
      case PCF:
        checkIfInputIsNotPresent(input.getCloudProviderType(), input.getPcfCloudProvider());
        updateSettingAttribute(settingAttribute, input.getPcfCloudProvider(), mutationContext.getAccountId());
        break;
      case SPOT_INST:
        checkIfInputIsNotPresent(input.getCloudProviderType(), input.getSpotInstCloudProvider());
        updateSettingAttribute(settingAttribute, input.getSpotInstCloudProvider(), mutationContext.getAccountId());
        break;
      case GCP:
        checkIfInputIsNotPresent(input.getCloudProviderType(), input.getGcpCloudProvider());
        updateSettingAttribute(settingAttribute, input.getGcpCloudProvider(), mutationContext.getAccountId());
        break;
      case KUBERNETES_CLUSTER:
        checkIfInputIsNotPresent(input.getCloudProviderType(), input.getK8sCloudProvider());
        updateSettingAttribute(settingAttribute, input.getK8sCloudProvider(), mutationContext.getAccountId());
        break;
      default:
        throw new InvalidRequestException("Invalid cloud provider type");
    }

    settingAttribute =
        settingsService.updateWithSettingFields(settingAttribute, settingAttribute.getUuid(), GLOBAL_APP_ID);
    settingServiceHelper.updateSettingAttributeBeforeResponse(settingAttribute, false);
    return builder.cloudProvider(CloudProviderController.populateCloudProvider(settingAttribute).build()).build();
  }

  private void updateSettingAttribute(
      SettingAttribute settingAttribute, QLPcfCloudProviderInput input, String accountId) {
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();

    if (input.getEndpointUrl().isPresent()) {
      input.getEndpointUrl().getValue().ifPresent(pcfConfig::setEndpointUrl);
    }
    if (input.getUserName().isPresent()) {
      input.getUserName().getValue().ifPresent(pcfConfig::setUsername);
    }
    if (input.getPasswordSecretId().isPresent()) {
      input.getPasswordSecretId().getValue().ifPresent(pcfConfig::setEncryptedPassword);
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

  private void updateSettingAttribute(
      SettingAttribute settingAttribute, QLSpotInstCloudProviderInput input, String accountId) {
    SpotInstConfig config = (SpotInstConfig) settingAttribute.getValue();

    if (input.getAccountId().isPresent()) {
      input.getAccountId().getValue().ifPresent(config::setSpotInstAccountId);
    }
    if (input.getTokenSecretId().isPresent()) {
      input.getTokenSecretId().getValue().ifPresent(config::setEncryptedSpotInstToken);
    }
    settingAttribute.setValue(config);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttribute::setName);
    }

    if (input.getUsageScope().isPresent()) {
      QLUsageScope usageScope = input.getUsageScope().getValue().orElse(null);
      settingAttribute.setUsageRestrictions(usageScopeController.populateUsageRestrictions(usageScope, accountId));
    }
  }

  private void updateSettingAttribute(
      SettingAttribute settingAttribute, QLGcpCloudProviderInput input, String accountId) {
    GcpConfig config = (GcpConfig) settingAttribute.getValue();

    if (input.getServiceAccountKeySecretId().isPresent()) {
      input.getServiceAccountKeySecretId().getValue().ifPresent(config::setEncryptedServiceAccountKeyFileContent);
    }

    settingAttribute.setValue(config);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttribute::setName);
    }

    if (input.getUsageScope().isPresent()) {
      QLUsageScope usageScope = input.getUsageScope().getValue().orElse(null);
      settingAttribute.setUsageRestrictions(usageScopeController.populateUsageRestrictions(usageScope, accountId));
    }
  }

  private void updateSettingAttribute(
      SettingAttribute settingAttribute, QLK8sCloudProviderInput input, String accountId) {
    KubernetesClusterConfig config = (KubernetesClusterConfig) settingAttribute.getValue();

    if (input.getClusterDetailsType().isPresent()) {
      switch (input.getClusterDetailsType().getValue().orElse(null)) {
        case INHERIT_CLUSTER_DETAILS:
          config.setUseKubernetesDelegate(true);
          if (input.getInheritClusterDetails().isPresent()) {
            input.getInheritClusterDetails().getValue().ifPresent(
                clusterDetails -> { clusterDetails.getDelegateName().getValue().ifPresent(config::setDelegateName); });
          }
          break;
        case MANUAL_CLUSTER_DETAILS:
          config.setUseKubernetesDelegate(false);
          if (input.getManualClusterDetails().isPresent()) {
            input.getManualClusterDetails().getValue().ifPresent(clusterDetails -> {
              clusterDetails.getMasterUrl().getValue().ifPresent(config::setMasterUrl);

              clusterDetails.getType().getValue().ifPresent(type -> {
                switch (type) {
                  case USERNAME_AND_PASSWORD:
                    config.setAuthType(KubernetesClusterAuthType.USER_PASSWORD);
                    clusterDetails.getUsernameAndPassword().getValue().ifPresent(auth -> {
                      auth.getUserName().getValue().ifPresent(config::setUsername);
                      auth.getPasswordSecretId().getValue().ifPresent(config::setEncryptedPassword);
                    });
                    break;
                  case SERVICE_ACCOUNT_TOKEN:
                    config.setAuthType(KubernetesClusterAuthType.SERVICE_ACCOUNT);
                    clusterDetails.getServiceAccountToken().getValue().ifPresent(auth -> {
                      auth.getServiceAccountTokenSecretId().getValue().ifPresent(
                          config::setEncryptedServiceAccountToken);
                    });
                    break;
                  case OIDC_TOKEN:
                    config.setAuthType(KubernetesClusterAuthType.OIDC);
                    clusterDetails.getOidcToken().getValue().ifPresent(auth -> {
                      auth.getIdentityProviderUrl().getValue().ifPresent(config::setOidcIdentityProviderUrl);
                      auth.getUserName().getValue().ifPresent(config::setUsername);

                      auth.getPasswordSecretId().getValue().ifPresent(config::setEncryptedOidcPassword);
                      auth.getClientIdSecretId().getValue().ifPresent(config::setEncryptedOidcClientId);
                      auth.getClientSecretSecretId().getValue().ifPresent(config::setEncryptedOidcSecret);

                      auth.getScopes().getValue().ifPresent(config::setOidcScopes);
                    });
                    break;
                  case NONE:
                    config.setAuthType(KubernetesClusterAuthType.NONE);
                    clusterDetails.getNone().getValue().ifPresent(auth -> {
                      auth.getUserName().getValue().ifPresent(config::setUsername);
                      auth.getPasswordSecretId().getValue().ifPresent(config::setEncryptedPassword);

                      auth.getCaCertificateSecretId().getValue().ifPresent(config::setEncryptedCaCert);
                      auth.getClientCertificateSecretId().getValue().ifPresent(config::setEncryptedClientCert);
                      auth.getClientKeySecretId().getValue().ifPresent(config::setEncryptedClientKey);
                      auth.getClientKeyPassphraseSecretId().getValue().ifPresent(
                          config::setEncryptedClientKeyPassphrase);

                      auth.getClientKeyAlgorithm().getValue().ifPresent(config::setClientKeyAlgo);

                      auth.getServiceAccountTokenSecretId().getValue().ifPresent(
                          config::setEncryptedServiceAccountToken);
                    });
                    break;
                  default:
                    throw new InvalidRequestException("Invalid manual cluster details type");
                }
              });
            });
          }
          break;
        default:
          throw new InvalidRequestException("Invalid cluster details type");
      }
    }

    settingAttribute.setValue(config);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttribute::setName);
    }

    if (input.getUsageScope().isPresent()) {
      QLUsageScope usageScope = input.getUsageScope().getValue().orElse(null);
      settingAttribute.setUsageRestrictions(usageScopeController.populateUsageRestrictions(usageScope, accountId));
    }
  }
}
