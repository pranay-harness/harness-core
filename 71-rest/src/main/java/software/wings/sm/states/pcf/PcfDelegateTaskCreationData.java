package software.wings.sm.states.pcf;

import software.wings.beans.TaskType;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PcfDelegateTaskCreationData {
  private String accountId;
  private String appId;
  private TaskType taskType;
  private String waitId;
  private String envId;
  private String infrastructureMappingId;
  private Object[] parameters;
  private long timeout;
  private List<String> tagList;
  private String serviceTemplateId;
}
