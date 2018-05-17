package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;
import static software.wings.common.Constants.ALWAYS_TRUE_CRITERIA;

import software.wings.beans.DelegateTask;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class AlwaysTrueValidation extends AbstractDelegateValidateTask {
  public AlwaysTrueValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    return singletonList(DelegateConnectionResult.builder().criteria(ALWAYS_TRUE_CRITERIA).validated(true).build());
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(ALWAYS_TRUE_CRITERIA);
  }
}
