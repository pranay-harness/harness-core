package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class KmsValidation extends AbstractDelegateValidateTask {
  public KmsValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList("https://aws.amazon.com/");
  }
}
