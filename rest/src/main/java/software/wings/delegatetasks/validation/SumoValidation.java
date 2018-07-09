package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;
import software.wings.beans.SumoConfig;
import software.wings.service.impl.sumo.SumoDataCollectionInfo;
import software.wings.service.intfc.security.EncryptionConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class SumoValidation extends AbstractSecretManagerValidation {
  public SumoValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(
        Arrays.stream(getParameters())
            .filter(o -> o instanceof SumoDataCollectionInfo || o instanceof SumoConfig)
            .map(obj
                -> (obj instanceof SumoConfig ? (SumoConfig) obj : ((SumoDataCollectionInfo) obj).getSumoConfig())
                       .getSumoUrl())
            .findFirst()
            .orElse(null));
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    for (Object parmeter : getParameters()) {
      if (parmeter instanceof SumoDataCollectionInfo) {
        return ((SumoDataCollectionInfo) parmeter).getEncryptedDataDetails().get(0).getEncryptionConfig();
      }
    }
    return super.getEncryptionConfig();
  }
}
