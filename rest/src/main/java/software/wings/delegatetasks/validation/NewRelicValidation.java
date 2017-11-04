package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class NewRelicValidation extends AbstractDelegateValidateTask {
  public NewRelicValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof NewRelicDataCollectionInfo)
                             .map(info -> ((NewRelicDataCollectionInfo) info).getNewRelicConfig().getNewRelicUrl())
                             .findFirst()
                             .orElse(null));
  }
}
