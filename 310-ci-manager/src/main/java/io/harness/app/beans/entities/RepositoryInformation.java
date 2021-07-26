package io.harness.app.beans.entities;

import java.util.List;

import io.harness.ng.cdOverview.dto.AuthorInfo;
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
