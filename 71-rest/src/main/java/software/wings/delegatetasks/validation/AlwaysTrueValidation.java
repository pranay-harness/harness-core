package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.delegate.beans.DelegateTaskPackage;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class AlwaysTrueValidation extends AbstractDelegateValidateTask {
  private static final String ALWAYS_TRUE_CRITERIA = "ALWAYS_TRUE_CRITERIA";

  public AlwaysTrueValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
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
