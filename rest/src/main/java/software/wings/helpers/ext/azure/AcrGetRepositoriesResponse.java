package software.wings.helpers.ext.azure;

import java.util.List;

public class AcrGetRepositoriesResponse {
  private List<String> repositories;

  public List<String> getRepositories() {
    return repositories;
  }

  public void setRepositories(List<String> repositories) {
    this.repositories = repositories;
  }
}
