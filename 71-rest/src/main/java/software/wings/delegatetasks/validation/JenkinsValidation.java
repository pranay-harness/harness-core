package software.wings.delegatetasks.validation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.network.Http.connectableJenkinsHttpUrl;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.command.JenkinsTaskParams;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by brett on 11/2/17
 */
@OwnedBy(CDC)
public class JenkinsValidation extends AbstractDelegateValidateTask {
  public JenkinsValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return Arrays.stream(getParameters())
        .filter(o -> o instanceof JenkinsTaskParams || o instanceof JenkinsConfig)
        .map(obj
            -> (obj instanceof JenkinsConfig ? (JenkinsConfig) obj : ((JenkinsTaskParams) obj).getJenkinsConfig())
                   .getJenkinsUrl())
        .collect(Collectors.toList());
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    return getCriteria()
        .stream()
        .map(criteria
            -> DelegateConnectionResult.builder()
                   .criteria(criteria)
                   .validated(connectableJenkinsHttpUrl(criteria))
                   .build())
        .collect(Collectors.toList());
  }
}
