package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.DatadogConfig;
import software.wings.beans.DelegateTask;
import software.wings.service.impl.apm.APMDataCollectionInfo;
import software.wings.service.intfc.security.EncryptionConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class APMValidation extends AbstractSecretManagerValidation {
  public APMValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o
                                 -> o instanceof DatadogConfig || o instanceof APMVerificationConfig
                                     || o instanceof APMValidateCollectorConfig)
                             .map(obj -> {
                               if (obj instanceof DatadogConfig) {
                                 return ((DatadogConfig) obj).getUrl();
                               } else if (obj instanceof APMVerificationConfig) {
                                 return ((APMVerificationConfig) obj).getUrl();
                               } else {
                                 return ((APMValidateCollectorConfig) obj).getUrl();
                               }
                             })
                             .findFirst()
                             .orElse(null));
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    for (Object parmeter : getParameters()) {
      if (parmeter instanceof APMDataCollectionInfo) {
        return ((APMDataCollectionInfo) parmeter).getEncryptedDataDetails().get(0).getEncryptionConfig();
      }
    }
    return super.getEncryptionConfig();
  }
}
