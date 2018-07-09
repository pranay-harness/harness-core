package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DelegateTask;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;
import software.wings.service.intfc.security.EncryptionConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class AppdynamicsValidation extends AbstractSecretManagerValidation {
  public AppdynamicsValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
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
