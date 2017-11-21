package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;
import software.wings.beans.GitConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by anubhaw on 11/6/17.
 */
public class GitValidation extends AbstractDelegateValidateTask {
  public GitValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof GitConfig)
                             .map(config -> ((GitConfig) config).getRepoUrl())
                             .findFirst()
                             .orElse(null));
  }
}
