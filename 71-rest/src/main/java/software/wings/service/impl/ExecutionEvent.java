package software.wings.service.impl;

import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity(value = "executionQueue", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class ExecutionEvent extends Queuable {
  private String appId;
  private String workflowId;
  private List<String> infraMappingIds;
  private List<String> infraDefinitionIds;
}
