package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.security.encryption.EncryptionConfig;
import software.wings.beans.DynaTraceConfig;
import software.wings.service.impl.dynatrace.DynaTraceDataCollectionInfo;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class DynaTraceValidation extends AbstractSecretManagerValidation {
  public DynaTraceValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
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
