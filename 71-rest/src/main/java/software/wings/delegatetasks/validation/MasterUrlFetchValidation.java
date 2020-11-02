package software.wings.delegatetasks.validation;

import static io.harness.network.Http.connectableHttpUrl;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.MasterUrlFetchTaskParameter;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class MasterUrlFetchValidation extends AbstractDelegateValidateTask {
  private static final String LOCAL_ENCRYPTION_CONFIG = "LOCAL_ENCRYPTION_CONFIG";
  public MasterUrlFetchValidation(
      String delegateId, DelegateTaskPackage delegateTaskPackage, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTaskPackage, consumer);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    try {
      String criteria = getCriteria().get(0);
      if (criteria.equals(LOCAL_ENCRYPTION_CONFIG)) {
        return singletonList(DelegateConnectionResult.builder().criteria(criteria).validated(true).build());
      }
      return singletonList(
          DelegateConnectionResult.builder().criteria(criteria).validated(connectableHttpUrl(criteria)).build());
    } catch (Exception e) {
      return emptyList();
    }
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(getCriteria((MasterUrlFetchTaskParameter) getParameters()[2]));
  }

  public String getCriteria(MasterUrlFetchTaskParameter taskParameter) {
    ContainerServiceParams containerServiceParams = taskParameter.getContainerServiceParams();
    EncryptedDataDetail encryptedDataDetail = containerServiceParams.getEncryptionDetails().get(0);
    EncryptionConfig encryptionConfig = encryptedDataDetail.getEncryptionConfig();

    if (encryptionConfig == null) {
      log.error("[MasterUrlValidation] No criteria Found. Should Not Happen");
      return null;
    } else if (encryptionConfig instanceof LocalEncryptionConfig) {
      return LOCAL_ENCRYPTION_CONFIG;
    } else {
      String masterValidationUrl = encryptionConfig.getEncryptionServiceUrl();
      log.info("[MasterUrlValidation] Criteria Found for validation with url" + masterValidationUrl);
      return masterValidationUrl;
    }
  }
}
