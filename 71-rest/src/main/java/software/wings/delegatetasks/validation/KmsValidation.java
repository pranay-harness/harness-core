package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import com.google.common.base.Preconditions;

import software.wings.beans.DelegateTask;
import software.wings.beans.KmsConfig;
import software.wings.service.intfc.security.EncryptionConfig;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class KmsValidation extends AbstractSecretManagerValidation {
  public KmsValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    EncryptionConfig encryptionConfig = getEncryptionConfig();
    Preconditions.checkState(encryptionConfig instanceof KmsConfig, "wrong config " + encryptionConfig);
    KmsConfig kmsConfig = (KmsConfig) encryptionConfig;
    return singletonList(kmsConfig.getValidationCriteria());
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    return singletonList(super.validateSecretManager());
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    for (Object parameter : getParameters()) {
      if (parameter instanceof KmsConfig) {
        return (EncryptionConfig) parameter;
      }
    }
    return null;
  }
}
