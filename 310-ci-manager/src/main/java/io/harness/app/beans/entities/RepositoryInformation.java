package io.harness.app.beans.entities;

import io.harness.ng.core.dashboard.AuthorInfo;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RepositoryInformation {
  private List<String> repoName;
  private List<String> status;
  private List<Long> startTime;
  private List<Long> endTime;
  private List<String> commitMessage;
  private List<AuthorInfo> authorInfoList;
}
