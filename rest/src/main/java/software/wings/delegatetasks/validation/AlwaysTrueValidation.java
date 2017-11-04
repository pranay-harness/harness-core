package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class AlwaysTrueValidation extends AbstractDelegateValidateTask {
  private static final String ALWAYS_TRUE_CRITERIA = "ALWAYS_TRUE_CRITERIA";

  public AlwaysTrueValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    return singletonList(DelegateConnectionResult.builder().validated(true).criteria(ALWAYS_TRUE_CRITERIA).build());
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(ALWAYS_TRUE_CRITERIA);
  }
}
