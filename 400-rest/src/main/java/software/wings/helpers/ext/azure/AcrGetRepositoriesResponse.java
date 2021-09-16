package software.wings.helpers.ext.azure;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;

@OwnedBy(CDC)
@TargetModule(_960_API_SERVICES)
public class AcrGetRepositoriesResponse {
  private List<String> repositories;

  public List<String> getRepositories() {
    return repositories;
  }

  public void setRepositories(List<String> repositories) {
    this.repositories = repositories;
  }
}
