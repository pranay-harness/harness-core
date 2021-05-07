package io.harness.app.beans.entities;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RepositoryInformation {
  private List<String> repoName;
  private List<String> status;
  private List<String> time;
  private List<String> commitMessage;
}
