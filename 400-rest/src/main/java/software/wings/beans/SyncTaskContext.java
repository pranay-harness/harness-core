package software.wings.beans;

import io.harness.beans.EnvironmentType;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SyncTaskContext {
  private String accountId;
  private String appId;
  private String envId;
  private EnvironmentType envType;
  private String infrastructureMappingId;
  private String serviceId;
  private String infraStructureDefinitionId;
  private long timeout;
  private List<String> tags;
  private String correlationId;
  private String orgIdentifier;
  private String projectIdentifier;
}
