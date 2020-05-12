package software.wings.delegatetasks.validation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;

import java.util.List;
import java.util.function.Consumer;

@OwnedBy(CDC)
public class CustomArtifactSourceValidation extends AbstractDelegateValidateTask {
  private static final String ALWAYS_TRUE_CRITERIA = "ALWAYS_TRUE_CRITERIA";

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
