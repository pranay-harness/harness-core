package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.APMVerificationConfig;
import software.wings.beans.DatadogConfig;
import software.wings.beans.DelegateTask;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class APMValidation extends AbstractDelegateValidateTask {
  public APMValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof DatadogConfig || o instanceof APMVerificationConfig)
                             .map(obj -> {
                               if (obj instanceof DatadogConfig) {
                                 return ((DatadogConfig) obj).getUrl();
                               } else {
                                 return ((APMVerificationConfig) obj).getUrl();
                               }
                             })
                             .findFirst()
                             .orElse(null));
  }
}
