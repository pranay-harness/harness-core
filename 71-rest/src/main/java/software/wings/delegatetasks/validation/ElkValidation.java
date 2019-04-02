package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.beans.DelegateTask;
import io.harness.security.encryption.EncryptionConfig;
import software.wings.beans.ElkConfig;
import software.wings.service.impl.elk.ElkDataCollectionInfo;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class ElkValidation extends AbstractSecretManagerValidation {
  public ElkValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(
        Arrays.stream(getParameters())
            .filter(o -> o instanceof ElkDataCollectionInfo || o instanceof ElkConfig)
            .map(obj
                -> (obj instanceof ElkConfig ? (ElkConfig) obj : ((ElkDataCollectionInfo) obj).getElkConfig())
                       .getElkUrl())
            .findFirst()
            .orElse(null));
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    for (Object parmeter : getParameters()) {
      if (parmeter instanceof ElkDataCollectionInfo) {
        return ((ElkDataCollectionInfo) parmeter).getEncryptedDataDetails().get(0).getEncryptionConfig();
      }
    }
    return super.getEncryptionConfig();
  }
}
