package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DirectKubernetesToCloudProvider implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(DirectKubernetesToCloudProvider.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private EnvironmentService environmentService;
  @Inject private SecretManager secretManager;
  @Inject private EncryptionService encryptionService;
  @Inject private SettingsService settingsService;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  @Override
  public void migrate() {
    logger.info("Converting Direct Kubernetes to Cloud Providers");
    int totalCount = 0;
    List<Account> accounts = wingsPersistence.createQuery(Account.class, excludeAuthority).asList();
    for (Account account : accounts) {
      List<Application> apps =
          wingsPersistence.createQuery(Application.class).filter("accountId", account.getUuid()).asList();
      for (Application app : apps) {
        List<InfrastructureMapping> infraMappings = wingsPersistence.createQuery(InfrastructureMapping.class)
                                                        .filter("appId", app.getUuid())
                                                        .filter("infraMappingType", "DIRECT_KUBERNETES")
                                                        .filter("computeProviderType", "DIRECT")
                                                        .asList();

        if (isNotEmpty(infraMappings)) {
          logger.info("");
          logger.info("Account: [{}], App: [{}] has {} legacy direct kubernetes infra mappings.",
              account.getCompanyName(), app.getName(), infraMappings.size());
          logger.info("");
          totalCount += infraMappings.size();
          for (InfrastructureMapping infrastructureMapping : infraMappings) {
            DirectKubernetesInfrastructureMapping directInfra =
                (DirectKubernetesInfrastructureMapping) infrastructureMapping;
            String envId = directInfra.getEnvId();

            Environment env = null;
            try {
              env = environmentService.get(app.getUuid(), envId, false);
            } catch (Exception e) {
              logger.info("Deleting for missing env: [{}], Infra: [{}]", envId, directInfra.getName());
              wingsPersistence.delete(infrastructureMapping);
            }

            if (env != null) {
              try {
                logger.info("Migrating for Env: [{}], Infra: [{}]", env.getName(), directInfra.getName());
                List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(
                    (EncryptableSetting) infrastructureMapping, infrastructureMapping.getAppId(), null);
                encryptionService.decrypt((EncryptableSetting) infrastructureMapping, encryptionDetails);
                KubernetesClusterConfig kubernetesClusterConfig = new KubernetesClusterConfig();
                kubernetesClusterConfig.setMasterUrl(
                    ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getMasterUrl());
                kubernetesClusterConfig.setUsername(
                    ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getUsername());
                kubernetesClusterConfig.setPassword(
                    ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getPassword());
                kubernetesClusterConfig.setCaCert(
                    ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getCaCert());
                kubernetesClusterConfig.setClientCert(
                    ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getClientCert());
                kubernetesClusterConfig.setClientKey(
                    ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getClientKey());
                kubernetesClusterConfig.setClientKeyPassphrase(
                    ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getClientKeyPassphrase());
                kubernetesClusterConfig.setClientKeyAlgo(
                    ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getClientKeyAlgo());
                kubernetesClusterConfig.setAccountId(infrastructureMapping.getAccountId());

                SettingAttribute settingAttribute = new SettingAttribute();
                settingAttribute.setAccountId(infrastructureMapping.getAccountId());
                settingAttribute.setCategory(Category.CLOUD_PROVIDER);
                settingAttribute.setAppId(Base.GLOBAL_APP_ID);
                settingAttribute.setEnvId(Base.GLOBAL_ENV_ID);
                String cloudProviderName =
                    ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getClusterName() + "-"
                    + infrastructureMapping.getUuid() + "-Migrated-CloudProvider";
                settingAttribute.setName(cloudProviderName);
                settingAttribute.setValue(kubernetesClusterConfig);

                SettingAttribute savedSettingAttribute = settingsService.save(settingAttribute);

                infrastructureMapping.setComputeProviderType(savedSettingAttribute.getValue().getType());
                infrastructureMapping.setComputeProviderSettingId(savedSettingAttribute.getUuid());

                Map<String, Object> keyValuePairs = new HashMap<>();
                Set<String> fieldsToRemove = new HashSet<>();

                keyValuePairs.put("computeProviderType", savedSettingAttribute.getValue().getType());
                keyValuePairs.put("computeProviderSettingId", savedSettingAttribute.getUuid());
                keyValuePairs.put("computeProviderName", savedSettingAttribute.getName());

                wingsPersistence.updateFields(
                    infrastructureMapping.getClass(), infrastructureMapping.getUuid(), keyValuePairs, fieldsToRemove);

                logger.info("Successfully Migrated env: [{}], Infra: [{}], to CloudProvider: {}", envId,
                    directInfra.getName(), cloudProviderName);

              } catch (Exception e) {
                logger.info(
                    "Exception migrating env: [{}], Infra: [{}], Exception: {}", envId, directInfra.getName(), e);
              }
            }
          }
        }

        logger.info("");
        logger.info("Total migrated: {}", totalCount);
      }
    }
  }
}
