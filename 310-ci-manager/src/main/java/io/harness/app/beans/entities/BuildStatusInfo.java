package io.harness.app.beans.entities;

import io.harness.ng.cdOverview.dto.AuthorInfo;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BuildStatusInfo {
  private String piplineName;
  private String pipelineIdentifier;
  private String branch;
  private String commit;
  private String commitID;
  private AuthorInfo author;
  private long startTs;
  private String status;
  private long endTs;
}
