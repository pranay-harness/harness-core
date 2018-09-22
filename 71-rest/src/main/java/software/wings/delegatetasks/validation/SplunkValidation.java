package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;
import software.wings.beans.SplunkConfig;
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.service.intfc.security.EncryptionConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class SplunkValidation extends AbstractSecretManagerValidation {
  public SplunkValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof SplunkDataCollectionInfo || o instanceof SplunkConfig)
                             .map(obj
                                 -> (obj instanceof SplunkConfig ? (SplunkConfig) obj
                                                                 : ((SplunkDataCollectionInfo) obj).getSplunkConfig())
                                        .getSplunkUrl())
                             .findFirst()
                             .orElse(null));
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    for (Object parmeter : getParameters()) {
      if (parmeter instanceof SplunkDataCollectionInfo) {
        return ((SplunkDataCollectionInfo) parmeter).getEncryptedDataDetails().get(0).getEncryptionConfig();
      }
    }
    return super.getEncryptionConfig();
  }
}
