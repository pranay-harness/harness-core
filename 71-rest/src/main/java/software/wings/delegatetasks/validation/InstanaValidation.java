package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.beans.DelegateTask;
import software.wings.beans.InstanaConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class InstanaValidation extends AbstractSecretManagerValidation {
  public InstanaValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }
  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof InstanaConfig)
                             .map(obj -> ((InstanaConfig) obj).getInstanaUrl())
                             .findFirst()
                             .orElse(null));
  }
}
