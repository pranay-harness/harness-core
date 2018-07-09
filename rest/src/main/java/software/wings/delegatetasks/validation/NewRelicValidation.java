package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;
import software.wings.beans.NewRelicConfig;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.intfc.security.EncryptionConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class NewRelicValidation extends AbstractSecretManagerValidation {
  public NewRelicValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(
        Arrays.stream(getParameters())
            .filter(o -> o instanceof NewRelicDataCollectionInfo || o instanceof NewRelicConfig)
            .map(obj
                -> (obj instanceof NewRelicConfig ? (NewRelicConfig) obj
                                                  : ((NewRelicDataCollectionInfo) obj).getNewRelicConfig())
                       .getNewRelicUrl())
            .findFirst()
            .orElse(null));
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    for (Object parmeter : getParameters()) {
      if (parmeter instanceof NewRelicDataCollectionInfo) {
        return ((NewRelicDataCollectionInfo) parmeter).getEncryptedDataDetails().get(0).getEncryptionConfig();
      }
    }

    return super.getEncryptionConfig();
  }
}
