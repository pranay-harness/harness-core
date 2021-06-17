package io.harness.app.beans.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BuildActiveInfo {
  private String piplineName;
  private String branch;
  private String commit;
  private String commitID;
  private AuthorInfo author;
  private long startTs;
  private String status;
  private long endTs;
}
