package software.wings.helpers.ext.azure;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(CDC)
public class AcrGetRepositoryTagsResponse {
  private String name;
  private List<String> tags;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }
}
