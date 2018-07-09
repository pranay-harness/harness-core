package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;
import software.wings.beans.config.LogzConfig;
import software.wings.service.impl.logz.LogzDataCollectionInfo;
import software.wings.service.intfc.security.EncryptionConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class LogzValidation extends AbstractSecretManagerValidation {
  public LogzValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(
        Arrays.stream(getParameters())
            .filter(o -> o instanceof LogzDataCollectionInfo || o instanceof LogzConfig)
            .map(obj
                -> (obj instanceof LogzConfig ? (LogzConfig) obj : ((LogzDataCollectionInfo) obj).getLogzConfig())
                       .getLogzUrl())
            .findFirst()
            .orElse(null));
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    for (Object parmeter : getParameters()) {
      if (parmeter instanceof LogzDataCollectionInfo) {
        return ((LogzDataCollectionInfo) parmeter).getEncryptedDataDetails().get(0).getEncryptionConfig();
      }
    }
    return super.getEncryptionConfig();
  }
}
