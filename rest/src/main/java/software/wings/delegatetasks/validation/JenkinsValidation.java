package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTask;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.command.JenkinsTaskParams;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class JenkinsValidation extends AbstractDelegateValidateTask {
  public JenkinsValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(
        Arrays.stream(getParameters())
            .filter(o -> o instanceof JenkinsTaskParams || o instanceof JenkinsConfig)
            .map(obj
                -> (obj instanceof JenkinsConfig ? (JenkinsConfig) obj : ((JenkinsTaskParams) obj).getJenkinsConfig())
                       .getJenkinsUrl())
            .findFirst()
            .orElse(null));
  }
}
