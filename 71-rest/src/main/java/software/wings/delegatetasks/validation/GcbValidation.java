package software.wings.delegatetasks.validation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import software.wings.beans.command.GcbTaskParams;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
@OwnedBy(CDC)
public class GcbValidation extends AbstractDelegateValidateTask {
  public GcbValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return stream(getParameters())
        .filter(GcbTaskParams.class ::isInstance)
        .map(GcbTaskParams.class ::cast)
        .map(GcbTaskParams::getBuildUrl)
        .collect(toCollection(LinkedList::new));
  }
}
