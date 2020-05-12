package software.wings.delegatetasks.validation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import software.wings.beans.config.NexusConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
@OwnedBy(CDC)
public class NexusValidation extends AbstractDelegateValidateTask {
  public NexusValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof NexusConfig)
                             .map(config -> ((NexusConfig) config).getNexusUrl())
                             .findFirst()
                             .orElse(null));
  }
}
