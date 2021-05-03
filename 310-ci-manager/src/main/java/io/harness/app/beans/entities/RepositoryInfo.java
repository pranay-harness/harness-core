package io.harness.app.beans.entities;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RepositoryInfo {
  private String name;
  private long buildCount;
  private double percentSuccess;
  private double successRate;
  private String lastCommit;
  private String lastStatus;
  private List<RepositoryBuildInfo> countList;
  private String time;
}
