package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EnvironmentType;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@TargetModule(HarnessModule._957_CG_BEANS)
@OwnedBy(CDC)
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
  private boolean ngTask;
}
