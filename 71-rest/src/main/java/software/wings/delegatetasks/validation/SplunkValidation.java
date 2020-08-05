package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.SplunkConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class SplunkValidation extends AbstractSecretManagerValidation {
  public SplunkValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof SplunkConfig)
                             .map(obj -> ((SplunkConfig) obj).getSplunkUrl())
                             .findFirst()
                             .orElse(null));
  }
}
