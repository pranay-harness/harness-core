package software.wings.service.impl;

import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import software.wings.core.queue.Queuable;

import java.util.List;

@Entity(value = "executionQueue", noClassnameStored = true)
@Builder
@Data
public class ExecutionEvent extends Queuable {
  private String appId;
  private String workflowId;
  private List<String> infraMappingIds;
}
