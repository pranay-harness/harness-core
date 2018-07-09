package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;
import software.wings.beans.DynaTraceConfig;
import software.wings.service.impl.dynatrace.DynaTraceDataCollectionInfo;
import software.wings.service.intfc.security.EncryptionConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class DynaTraceValidation extends AbstractSecretManagerValidation {
  public DynaTraceValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(
        Arrays.stream(getParameters())
            .filter(o -> o instanceof DynaTraceDataCollectionInfo || o instanceof DynaTraceConfig)
            .map(obj
                -> (obj instanceof DynaTraceConfig ? (DynaTraceConfig) obj
                                                   : ((DynaTraceDataCollectionInfo) obj).getDynaTraceConfig())
                       .getDynaTraceUrl())
            .findFirst()
            .orElse(null));
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    for (Object parmeter : getParameters()) {
      if (parmeter instanceof DynaTraceDataCollectionInfo) {
        return ((DynaTraceDataCollectionInfo) parmeter).getEncryptedDataDetails().get(0).getEncryptionConfig();
      }
    }
    return super.getEncryptionConfig();
  }
}
