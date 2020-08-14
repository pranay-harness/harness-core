package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.delegate.beans.DelegateTaskPackage;
import software.wings.beans.InstanaConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class InstanaValidation extends AbstractSecretManagerValidation {
  public InstanaValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
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
