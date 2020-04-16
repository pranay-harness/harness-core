package software.wings.security.encryption.setupusage.builders;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.expression.SecretString.SECRET_MASK;
import static software.wings.beans.ServiceVariable.ENCRYPTED_VALUE_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.SearchFilter;
import io.harness.exception.InvalidArgumentsException;
import lombok.NonNull;
import software.wings.beans.EntityType;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.EncryptionDetail;
import software.wings.security.encryption.setupusage.SecretSetupUsage;
import software.wings.security.encryption.setupusage.SecretSetupUsageBuilder;
import software.wings.service.intfc.ServiceVariableService;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Singleton
public class ServiceVariableSetupUsageBuilder implements SecretSetupUsageBuilder {
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private WingsPersistence wingsPersistence;

  public Set<SecretSetupUsage> buildSecretSetupUsages(@NonNull String accountId, @NonNull String secretId,
      @NonNull Map<String, Set<EncryptedDataParent>> parentsByParentIds, @NonNull EncryptionDetail encryptionDetail) {
    Set<String> parentIds = parentsByParentIds.keySet();
    List<ServiceVariable> serviceVariableList =
        serviceVariableService
            .list(aPageRequest()
                      .addFilter(ID_KEY, SearchFilter.Operator.IN, parentIds.toArray())
                      .addFilter(ACCOUNT_ID_KEY, SearchFilter.Operator.EQ, accountId)
                      .addFilter(ENCRYPTED_VALUE_KEY, SearchFilter.Operator.EQ, secretId)
                      .build())
            .getResponse();

    Set<SecretSetupUsage> secretSetupUsages = new HashSet<>();
    for (ServiceVariable serviceVariable : serviceVariableList) {
      if (serviceVariable.getEntityType() == EntityType.SERVICE_TEMPLATE) {
        serviceVariable.setServiceId(getServiceId(serviceVariable.getEntityId()));
      }
      serviceVariable.setValue(SECRET_MASK.toCharArray());
      serviceVariable.setEncryptionType(encryptionDetail.getEncryptionType());
      serviceVariable.setEncryptedBy(encryptionDetail.getSecretManagerName());
      secretSetupUsages.add(SecretSetupUsage.builder()
                                .entityId(serviceVariable.getUuid())
                                .type(serviceVariable.getSettingType())
                                .fieldName(ENCRYPTED_VALUE_KEY)
                                .entity(serviceVariable)
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
