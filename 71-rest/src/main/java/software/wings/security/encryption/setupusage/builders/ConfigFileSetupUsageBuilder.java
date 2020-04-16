package software.wings.security.encryption.setupusage.builders;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.exception.WingsException.USER_SRE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.SearchFilter;
import io.harness.exception.InvalidArgumentsException;
import lombok.NonNull;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.ConfigFileKeys;
import software.wings.beans.EntityType;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.EncryptionDetail;
import software.wings.security.encryption.setupusage.SecretSetupUsage;
import software.wings.security.encryption.setupusage.SecretSetupUsageBuilder;
import software.wings.service.intfc.ConfigService;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Singleton
public class ConfigFileSetupUsageBuilder implements SecretSetupUsageBuilder {
  @Inject private ConfigService configService;
  @Inject private WingsPersistence wingsPersistence;

  public Set<SecretSetupUsage> buildSecretSetupUsages(String accountId, String secretId,
      Map<String, Set<EncryptedDataParent>> parentsByParentIds, EncryptionDetail encryptionDetail) {
    Set<String> parentIds = parentsByParentIds.keySet();
    List<ConfigFile> configFileList =
        configService
            .list(aPageRequest()
                      .addFilter(ID_KEY, SearchFilter.Operator.IN, parentIds.toArray())
                      .addFilter(ACCOUNT_ID_KEY, SearchFilter.Operator.EQ, accountId)
                      .addFilter(ConfigFileKeys.encryptedFileId, SearchFilter.Operator.EQ, secretId)
                      .build())
            .getResponse();

    Set<SecretSetupUsage> secretSetupUsages = new HashSet<>();
    for (ConfigFile configFile : configFileList) {
      if (configFile.getEntityType() == EntityType.SERVICE_TEMPLATE) {
        configFile.setServiceId(getServiceId(configFile.getEntityId()));
      }
      configFile.setEncryptionType(encryptionDetail.getEncryptionType());
      configFile.setEncryptedBy(encryptionDetail.getSecretManagerName());
      secretSetupUsages.add(SecretSetupUsage.builder()
                                .entityId(configFile.getUuid())
                                .type(configFile.getSettingType())
                                .fieldName(ConfigFileKeys.encryptedFileId)
                                .entity(configFile)
                                .build());
    }
    return secretSetupUsages;
  }

  private String getServiceId(@NonNull String serviceTemplateId) {
    return Optional.ofNullable(wingsPersistence.get(ServiceTemplate.class, serviceTemplateId))
        .<InvalidArgumentsException>orElseThrow(() -> {
          throw new InvalidArgumentsException(
              String.format("Can't find service template %s for service variable", serviceTemplateId), USER_SRE);
        })
        .getServiceId();
  }
}
