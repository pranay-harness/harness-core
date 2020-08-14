package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.security.encryption.EncryptionConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class AppdynamicsValidation extends AbstractSecretManagerValidation {
  public AppdynamicsValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(
        Arrays.stream(getParameters())
            .filter(o -> o instanceof AppdynamicsDataCollectionInfo || o instanceof AppDynamicsConfig)
            .map(obj
                -> (obj instanceof AppDynamicsConfig ? (AppDynamicsConfig) obj
                                                     : ((AppdynamicsDataCollectionInfo) obj).getAppDynamicsConfig())
                       .getControllerUrl())
            .findFirst()
            .orElse(null));
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    for (Object parmeter : getParameters()) {
      if (parmeter instanceof AppdynamicsDataCollectionInfo) {
        return ((AppdynamicsDataCollectionInfo) parmeter).getEncryptedDataDetails().get(0).getEncryptionConfig();
      }
    }
    return super.getEncryptionConfig();
  }
}
