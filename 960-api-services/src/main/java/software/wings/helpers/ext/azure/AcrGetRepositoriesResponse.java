package software.wings.helpers.ext.azure;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(CDC)
public class AcrGetRepositoriesResponse {
  private List<String> repositories;

  public List<String> getRepositories() {
    return repositories;
  }

  public void setRepositories(List<String> repositories) {
    this.repositories = repositories;
  }
}
