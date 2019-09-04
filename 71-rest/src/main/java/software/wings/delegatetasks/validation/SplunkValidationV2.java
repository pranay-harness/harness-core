package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.beans.DelegateTask;
import io.harness.security.encryption.EncryptionConfig;
import software.wings.beans.SplunkConfig;
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.service.impl.splunk.SplunkDataCollectionInfoV2;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
// TODO: TODO figure out a way to move this to common place with data collection info or generic types (Type safe)
public class SplunkValidationV2 extends AbstractSecretManagerValidation {
  public SplunkValidationV2(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof SplunkDataCollectionInfoV2 || o instanceof SplunkConfig)
                             .map(obj
                                 -> (obj instanceof SplunkConfig ? (SplunkConfig) obj
                                                                 : ((SplunkDataCollectionInfoV2) obj).getSplunkConfig())
                                        .getSplunkUrl())
                             .findFirst()
                             .orElse(null));
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    for (Object parameter : getParameters()) {
      if (parameter instanceof SplunkDataCollectionInfo) {
        return ((SplunkDataCollectionInfoV2) parameter).getEncryptedDataDetails().get(0).getEncryptionConfig();
      }
    }
    return super.getEncryptionConfig();
  }
}
