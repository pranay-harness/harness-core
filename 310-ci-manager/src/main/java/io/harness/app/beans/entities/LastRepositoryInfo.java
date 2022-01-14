package io.harness.app.beans.entities;

import io.harness.ng.core.dashboard.AuthorInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LastRepositoryInfo {
  private Long StartTime;
  private Long EndTime;
  private String status;
  private AuthorInfo author;
  private String commit;
}
