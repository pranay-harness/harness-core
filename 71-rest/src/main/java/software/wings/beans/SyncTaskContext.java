package software.wings.beans;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SyncTaskContext {
  private String accountId;
  private String appId;
  private String envId;
  private String infrastructureMappingId;
  private String infraStructureDefinitionId;
  private long timeout;
  private List<String> tags;
  private String correlationId;
}
