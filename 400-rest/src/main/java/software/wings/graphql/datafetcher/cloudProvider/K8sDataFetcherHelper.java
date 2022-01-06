/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.KubernetesClusterAuthType;
import io.harness.utils.RequestField;

import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesClusterConfig.KubernetesClusterConfigBuilder;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLInheritClusterDetails;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLK8sCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLUpdateInheritClusterDetails;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLUpdateK8sCloudProviderInput;
import software.wings.graphql.schema.type.secrets.QLUsageScope;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;

@Singleton
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class K8sDataFetcherHelper {
  @Inject private UsageScopeController usageScopeController;

  public SettingAttribute toSettingAttribute(QLK8sCloudProviderInput input, String accountId) {
    KubernetesClusterConfigBuilder configBuilder = KubernetesClusterConfig.builder().accountId(accountId);

    if (input.getSkipValidation().isPresent()) {
      input.getSkipValidation().getValue().ifPresent(configBuilder::skipValidation);
    }

    SettingAttribute.Builder settingAttributeBuilder =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withCategory(
            SettingAttribute.SettingCategory.SETTING);

    if (input.getClusterDetailsType().isPresent()) {
      switch (input.getClusterDetailsType().getValue().orElseThrow(
          () -> new InvalidRequestException("No cluster details type provided"))) {
        case INHERIT_CLUSTER_DETAILS:
          configBuilder.useKubernetesDelegate(true);
          if (input.getInheritClusterDetails().isPresent()) {
            input.getInheritClusterDetails().getValue().ifPresent(clusterDetails -> {
              clusterDetails.getDelegateName().getValue().ifPresent(configBuilder::delegateName);
              if (!clusterDetails.getDelegateSelectors().getValue().isPresent()) {
                clusterDetails.getDelegateName().getValue().ifPresent(
                    delegateName -> configBuilder.delegateSelectors(Collections.singleton(delegateName)));
              } else {
                clusterDetails.getDelegateSelectors().getValue().ifPresent(configBuilder::delegateSelectors);
              }
            });

            QLInheritClusterDetails inheritClusterDetails = input.getInheritClusterDetails().getValue().orElse(null);
            RequestField<QLUsageScope> usageRestrictions = inheritClusterDetails.getUsageScope();
            if (usageRestrictions != null && usageRestrictions.isPresent()) {
              settingAttributeBuilder.withUsageRestrictions(
                  usageScopeController.populateUsageRestrictions(usageRestrictions.getValue().orElse(null), accountId));
            }
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
                      validateUsernameFields(auth.getUserName(), auth.getUserNameSecretId(), false);
                      auth.getUserName().getValue().map(String::toCharArray).ifPresent(username -> {
                        configBuilder.username(username);
                        configBuilder.useEncryptedUsername(false);
                      });
                      auth.getUserNameSecretId().getValue().ifPresent(usernameSecretId -> {
                        configBuilder.encryptedUsername(usernameSecretId);
                        configBuilder.useEncryptedUsername(true);
                      });
                      auth.getPasswordSecretId().getValue().ifPresent(configBuilder::encryptedPassword);
                    });
                    break;
                  case SERVICE_ACCOUNT_TOKEN:
                    configBuilder.authType(KubernetesClusterAuthType.SERVICE_ACCOUNT);
                    clusterDetails.getServiceAccountToken().getValue().ifPresent(auth
                        -> auth.getServiceAccountTokenSecretId().getValue().ifPresent(
                            configBuilder::encryptedServiceAccountToken));
                    break;
                  case OIDC_TOKEN:
                    configBuilder.authType(KubernetesClusterAuthType.OIDC);
                    clusterDetails.getOidcToken().getValue().ifPresent(auth -> {
                      auth.getIdentityProviderUrl().getValue().ifPresent(configBuilder::oidcIdentityProviderUrl);
                      auth.getUserName().getValue().ifPresent(configBuilder::oidcUsername);

                      auth.getPasswordSecretId().getValue().ifPresent(configBuilder::encryptedOidcPassword);
                      auth.getClientIdSecretId().getValue().ifPresent(configBuilder::encryptedOidcClientId);
                      auth.getClientSecretSecretId().getValue().ifPresent(configBuilder::encryptedOidcSecret);

                      auth.getScopes().getValue().ifPresent(configBuilder::oidcScopes);
                    });
                    break;
                  case CUSTOM:
                    configBuilder.authType(KubernetesClusterAuthType.NONE);

                    clusterDetails.getNone().getValue().ifPresent(auth -> {
                      auth.getUserName().getValue().map(String::toCharArray).ifPresent(configBuilder::username);
                      auth.getPasswordSecretId().getValue().ifPresent(configBuilder::encryptedPassword);

                      auth.getCaCertificateSecretId().getValue().ifPresent(configBuilder::encryptedCaCert);
                      auth.getClientCertificateSecretId().getValue().ifPresent(configBuilder::encryptedClientCert);
                      auth.getClientKeySecretId().getValue().ifPresent(configBuilder::encryptedClientKey);
                      auth.getClientKeyPassphraseSecretId().getValue().ifPresent(
                          configBuilder::encryptedClientKeyPassphrase);

                      auth.getClientKeyAlgorithm().getValue().ifPresent(configBuilder::clientKeyAlgo);

                      auth.getServiceAccountTokenSecretId().getValue().ifPresent(
                          configBuilder::encryptedServiceAccountToken);
                      RequestField<QLUsageScope> usageRestrictions = auth.getUsageScope();
                      if (usageRestrictions != null && usageRestrictions.isPresent()) {
                        checkIfUsageScopeCanBeCreatedOrUpdated(configBuilder.build());
                        settingAttributeBuilder.withUsageRestrictions(usageScopeController.populateUsageRestrictions(
                            usageRestrictions.getValue().orElse(null), accountId));
                      }
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

    settingAttributeBuilder.withValue(configBuilder.build());

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttributeBuilder::withName);
    }

    return settingAttributeBuilder.build();
  }

  public void updateSettingAttribute(
      SettingAttribute settingAttribute, QLUpdateK8sCloudProviderInput input, String accountId) {
    KubernetesClusterConfig config = (KubernetesClusterConfig) settingAttribute.getValue();

    if (input.getSkipValidation().isPresent()) {
      input.getSkipValidation().getValue().ifPresent(config::setSkipValidation);
    }

    if (input.getClusterDetailsType().isPresent()) {
      switch (input.getClusterDetailsType().getValue().orElseThrow(
          () -> new InvalidRequestException("No cluster details type provided"))) {
        case INHERIT_CLUSTER_DETAILS:
          config.setUseKubernetesDelegate(true);
          if (input.getInheritClusterDetails().isPresent()) {
            input.getInheritClusterDetails().getValue().ifPresent(clusterDetails -> {
              clusterDetails.getDelegateName().getValue().ifPresent(config::setDelegateName);
              if (!clusterDetails.getDelegateSelectors().getValue().isPresent()) {
                clusterDetails.getDelegateName().getValue().ifPresent(
                    delegateName -> config.setDelegateSelectors(Collections.singleton(delegateName)));
              } else {
                clusterDetails.getDelegateSelectors().getValue().ifPresent(config::setDelegateSelectors);
              }
            });

            QLUpdateInheritClusterDetails inheritClusterDetails =
                input.getInheritClusterDetails().getValue().orElseThrow(
                    () -> new InvalidRequestException(" No Inherit cluster details supplied"));
            RequestField<QLUsageScope> usageRestrictions = inheritClusterDetails.getUsageScope();
            if (usageRestrictions != null && usageRestrictions.isPresent()) {
              settingAttribute.setUsageRestrictions(
                  usageScopeController.populateUsageRestrictions(usageRestrictions.getValue().orElse(null), accountId));
            }
          }
          break;
        case MANUAL_CLUSTER_DETAILS:
          config.setUseKubernetesDelegate(false);
          config.setDelegateName(null);
          config.setDelegateSelectors(null);
          if (input.getManualClusterDetails().isPresent()) {
            input.getManualClusterDetails().getValue().ifPresent(clusterDetails -> {
              clusterDetails.getMasterUrl().getValue().ifPresent(config::setMasterUrl);

              clusterDetails.getType().getValue().ifPresent(type -> {
                switch (type) {
                  case USERNAME_AND_PASSWORD:
                    config.setAuthType(KubernetesClusterAuthType.USER_PASSWORD);
                    clusterDetails.getUsernameAndPassword().getValue().ifPresent(auth -> {
                      validateUsernameFields(auth.getUserName(), auth.getUserNameSecretId(), true);
                      auth.getUserName().getValue().map(String::toCharArray).ifPresent(username -> {
                        config.setUsername(username);
                        config.setEncryptedUsername(null);
                        config.setUseEncryptedUsername(false);
                      });
                      auth.getUserNameSecretId().getValue().ifPresent(usernameSecretId -> {
                        config.setUsername(null);
                        config.setEncryptedUsername(usernameSecretId);
                        config.setUseEncryptedUsername(true);
                      });
                      auth.getPasswordSecretId().getValue().ifPresent(config::setEncryptedPassword);
                    });
                    break;
                  case SERVICE_ACCOUNT_TOKEN:
                    config.setAuthType(KubernetesClusterAuthType.SERVICE_ACCOUNT);
                    clusterDetails.getServiceAccountToken().getValue().ifPresent(auth
                        -> auth.getServiceAccountTokenSecretId().getValue().ifPresent(
                            config::setEncryptedServiceAccountToken));
                    break;
                  case OIDC_TOKEN:
                    config.setAuthType(KubernetesClusterAuthType.OIDC);
                    clusterDetails.getOidcToken().getValue().ifPresent(auth -> {
                      auth.getIdentityProviderUrl().getValue().ifPresent(config::setOidcIdentityProviderUrl);
                      auth.getUserName().getValue().ifPresent(config::setOidcUsername);

                      auth.getPasswordSecretId().getValue().ifPresent(config::setEncryptedOidcPassword);
                      auth.getClientIdSecretId().getValue().ifPresent(config::setEncryptedOidcClientId);
                      auth.getClientSecretSecretId().getValue().ifPresent(config::setEncryptedOidcSecret);

                      auth.getScopes().getValue().ifPresent(config::setOidcScopes);
                    });
                    break;
                  case CUSTOM:
                    config.setAuthType(KubernetesClusterAuthType.NONE);
                    clusterDetails.getNone().getValue().ifPresent(auth -> {
                      auth.getUserName().getValue().map(String::toCharArray).ifPresent(config::setUsername);
                      auth.getPasswordSecretId().getValue().ifPresent(config::setEncryptedPassword);

                      auth.getCaCertificateSecretId().getValue().ifPresent(config::setEncryptedCaCert);
                      auth.getClientCertificateSecretId().getValue().ifPresent(config::setEncryptedClientCert);
                      auth.getClientKeySecretId().getValue().ifPresent(config::setEncryptedClientKey);
                      auth.getClientKeyPassphraseSecretId().getValue().ifPresent(
                          config::setEncryptedClientKeyPassphrase);

                      auth.getClientKeyAlgorithm().getValue().ifPresent(config::setClientKeyAlgo);

                      auth.getServiceAccountTokenSecretId().getValue().ifPresent(
                          config::setEncryptedServiceAccountToken);
                      RequestField<QLUsageScope> usageRestrictions = auth.getUsageScope();
                      if (usageRestrictions != null && usageRestrictions.isPresent()) {
                        checkIfUsageScopeCanBeCreatedOrUpdated(config);
                        settingAttribute.setUsageRestrictions(usageScopeController.populateUsageRestrictions(
                            usageRestrictions.getValue().orElse(null), accountId));
                      }
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
  }

  private void checkIfUsageScopeCanBeCreatedOrUpdated(KubernetesClusterConfig config) {
    if (theCloudProviderUsesSecretId(config)) {
      throw new InvalidRequestException(
          "The usage scope should not be provided, when a secretId is provided in the api the scope will be automatically inherited from the secret");
    }
  }

  private boolean theCloudProviderUsesSecretId(KubernetesClusterConfig finalConfig) {
    if (finalConfig.getEncryptedUsername() != null) {
      return true;
    }

    if (finalConfig.getEncryptedPassword() != null) {
      return true;
    }

    if (finalConfig.getEncryptedCaCert() != null) {
      return true;
    }

    if (finalConfig.getEncryptedCaCert() != null) {
      return true;
    }

    if (finalConfig.getEncryptedCaCert() != null) {
      return true;
    }

    if (finalConfig.getEncryptedClientKeyPassphrase() != null) {
      return true;
    }

    if (finalConfig.getEncryptedServiceAccountToken() != null) {
      return true;
    }

    return false;
  }

  private void validateUsernameFields(
      RequestField<String> userName, RequestField<String> userNameSecretId, boolean isUpdate) {
    if (userName.getValue().isPresent() && userNameSecretId.getValue().isPresent()) {
      throw new InvalidRequestException("Cannot set both value and secret reference for username field", USER);
    }

    if (!isUpdate && !userName.getValue().isPresent() && !userNameSecretId.getValue().isPresent()) {
      throw new InvalidRequestException("One of fields 'userName' or 'userNameSecretId' is required", USER);
    }
  }
}
