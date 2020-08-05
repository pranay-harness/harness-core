package software.wings.delegatetasks.validation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.artifact.ArtifactStreamAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
@OwnedBy(CDC)
public class GcrValidation extends AbstractDelegateValidateTask {
  public GcrValidation(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTaskPackage, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof ArtifactStreamAttributes)
                             .map(config -> getUrl(((ArtifactStreamAttributes) config).getRegistryHostName()))
                             .findFirst()
                             .orElse(null));
  }

  private String getUrl(String gcrHostName) {
    return "https://" + gcrHostName + (gcrHostName.endsWith("/") ? "" : "/");
  }
}
