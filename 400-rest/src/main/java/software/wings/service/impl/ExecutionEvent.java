package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import io.harness.queue.Queuable;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity(value = "executionQueue", noClassnameStored = true)
@HarnessEntity(exportable = false)
@StoreIn(DbAliases.CG_MANAGER)
public class ExecutionEvent extends Queuable {
  private String appId;
  private String workflowId;
  private List<String> infraMappingIds;
  private List<String> infraDefinitionIds;
}
