package software.wings.delegatetasks.validation;

import com.google.common.collect.Lists;

import io.harness.beans.DelegateTask;

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
}
