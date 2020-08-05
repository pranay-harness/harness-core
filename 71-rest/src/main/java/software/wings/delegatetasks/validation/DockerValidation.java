package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.DockerConfig;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class DockerValidation extends AbstractDelegateValidateTask {
  public DockerValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof DockerConfig)
                             .map(config -> ((DockerConfig) config).getDockerRegistryUrl())
                             .findFirst()
                             .orElse(null));
  }
}
