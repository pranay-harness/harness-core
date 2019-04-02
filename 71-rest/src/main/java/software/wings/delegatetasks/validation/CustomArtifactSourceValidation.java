package software.wings.delegatetasks.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static software.wings.common.Constants.ALWAYS_TRUE_CRITERIA;

import io.harness.beans.DelegateTask;

import java.util.List;
import java.util.function.Consumer;

public class CustomArtifactSourceValidation extends AbstractDelegateValidateTask {
  public CustomArtifactSourceValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }
  @Override
  public List<String> getCriteria() {
    return asList(ALWAYS_TRUE_CRITERIA);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    return singletonList(DelegateConnectionResult.builder().criteria(ALWAYS_TRUE_CRITERIA).validated(true).build());
  }
}
