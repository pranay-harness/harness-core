package software.wings.delegatetasks.validation;

import com.google.common.collect.Lists;

import io.harness.delegate.beans.DelegateTaskPackage;

import java.util.List;
import java.util.function.Consumer;

public class CVDataCollectionValidator extends AbstractSecretManagerValidation {
  CVDataCollectionValidator(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }
  @Override
  public List<String> getCriteria() {
    return Lists.newArrayList("https://google.com");
  }
}
