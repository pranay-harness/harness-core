package software.wings.beans;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SyncTaskContext {
  private String accountId;
  private String appId;
  private String envId;
  private String infrastructureMappingId;
  private long timeout;
  private List<String> tags;
  private String correlationId;
}