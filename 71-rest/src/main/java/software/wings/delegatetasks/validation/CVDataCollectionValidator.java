package software.wings.delegatetasks.validation;

import com.google.common.collect.Lists;

import io.harness.beans.DelegateTask;
import io.harness.security.encryption.EncryptionConfig;
import software.wings.service.impl.analysis.DataCollectionInfoV2;

import java.util.List;
import java.util.function.Consumer;

public class CVDataCollectionValidator extends AbstractSecretManagerValidation {
  CVDataCollectionValidator(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }
  @Override
  public List<String> getCriteria() {
    return Lists.newArrayList("https://google.com");
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    DataCollectionInfoV2 dataCollectionInfoV2 = (DataCollectionInfoV2) getParameters()[0];
    return dataCollectionInfoV2.getEncryptionConfig().orElse(super.getEncryptionConfig());
  }
}
