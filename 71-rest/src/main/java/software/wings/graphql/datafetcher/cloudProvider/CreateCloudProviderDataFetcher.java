package software.wings.graphql.datafetcher.cloudProvider;

import static software.wings.graphql.datafetcher.cloudProvider.CloudProviderController.checkIfInputIsNotPresent;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.GcpConfig;
import software.wings.beans.GcpConfig.GcpConfigBuilder;
import software.wings.beans.KubernetesClusterAuthType;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesClusterConfig.KubernetesClusterConfigBuilder;
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
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLK8sCloudProviderInput;
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
      case KUBERNETES_CLUSTER:
        checkIfInputIsNotPresent(input.getCloudProviderType(), input.getK8sCloudProvider());
        settingAttribute = toSettingAttribute(input.getK8sCloudProvider(), mutationContext.getAccountId());
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

  private SettingAttribute toSettingAttribute(QLK8sCloudProviderInput input, String accountId) {
    KubernetesClusterConfigBuilder configBuilder = KubernetesClusterConfig.builder().accountId(accountId);

    if (input.getSkipValidation().isPresent()) {
      input.getSkipValidation().getValue().ifPresent(configBuilder::skipValidation);
    }

    if (input.getClusterDetailsType().isPresent()) {
      switch (input.getClusterDetailsType().getValue().orElse(null)) {
        case INHERIT_CLUSTER_DETAILS:
          configBuilder.useKubernetesDelegate(true);
          if (input.getInheritClusterDetails().isPresent()) {
            input.getInheritClusterDetails().getValue().ifPresent(clusterDetails -> {
              clusterDetails.getDelegateName().getValue().ifPresent(configBuilder::delegateName);
            });
          }
          break;
        case MANUAL_CLUSTER_DETAILS:
          configBuilder.useKubernetesDelegate(false);
          if (input.getManualClusterDetails().isPresent()) {
            input.getManualClusterDetails().getValue().ifPresent(clusterDetails -> {
              clusterDetails.getMasterUrl().getValue().ifPresent(configBuilder::masterUrl);

              clusterDetails.getType().getValue().ifPresent(type -> {
                switch (type) {
                  case USERNAME_AND_PASSWORD:
                    configBuilder.authType(KubernetesClusterAuthType.USER_PASSWORD);
                    clusterDetails.getUsernameAndPassword().getValue().ifPresent(auth -> {
                      auth.getUserName().getValue().ifPresent(configBuilder::username);
                      auth.getPasswordSecretId().getValue().ifPresent(configBuilder::encryptedPassword);
                    });
                    break;
                  case SERVICE_ACCOUNT_TOKEN:
                    configBuilder.authType(KubernetesClusterAuthType.SERVICE_ACCOUNT);
                    clusterDetails.getServiceAccountToken().getValue().ifPresent(auth -> {
                      auth.getServiceAccountTokenSecretId().getValue().ifPresent(
                          configBuilder::encryptedServiceAccountToken);
                    });
                    break;
                  case OIDC_TOKEN:
                    configBuilder.authType(KubernetesClusterAuthType.OIDC);
                    clusterDetails.getOidcToken().getValue().ifPresent(auth -> {
                      auth.getIdentityProviderUrl().getValue().ifPresent(configBuilder::oidcIdentityProviderUrl);
                      auth.getUserName().getValue().ifPresent(configBuilder::username);

                      auth.getPasswordSecretId().getValue().ifPresent(configBuilder::encryptedOidcPassword);
                      auth.getClientIdSecretId().getValue().ifPresent(configBuilder::encryptedOidcClientId);
                      auth.getClientSecretSecretId().getValue().ifPresent(configBuilder::encryptedOidcSecret);

                      auth.getScopes().getValue().ifPresent(configBuilder::oidcScopes);
                    });
                    break;
                  case NONE:
                    configBuilder.authType(KubernetesClusterAuthType.NONE);
                    clusterDetails.getNone().getValue().ifPresent(auth -> {
                      auth.getUserName().getValue().ifPresent(configBuilder::username);
                      auth.getPasswordSecretId().getValue().ifPresent(configBuilder::encryptedPassword);

                      auth.getCaCertificateSecretId().getValue().ifPresent(configBuilder::encryptedCaCert);
                      auth.getClientCertificateSecretId().getValue().ifPresent(configBuilder::encryptedClientCert);
                      auth.getClientKeySecretId().getValue().ifPresent(configBuilder::encryptedClientKey);
                      auth.getClientKeyPassphraseSecretId().getValue().ifPresent(
                          configBuilder::encryptedClientKeyPassphrase);

                      auth.getClientKeyAlgorithm().getValue().ifPresent(configBuilder::clientKeyAlgo);

                      auth.getServiceAccountTokenSecretId().getValue().ifPresent(
                          configBuilder::encryptedServiceAccountToken);
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
