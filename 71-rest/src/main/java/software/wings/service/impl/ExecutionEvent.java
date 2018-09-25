package software.wings.service.impl;

import io.harness.queue.Queuable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

import java.util.List;

@Entity(value = "executionQueue", noClassnameStored = true)
@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class ExecutionEvent extends Queuable {
  private String appId;
  private String workflowId;
  private List<String> infraMappingIds;
}
