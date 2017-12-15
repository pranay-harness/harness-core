package software.wings.service.impl.newrelic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;

import java.util.List;

/**
 * Created by sriram_parthasarathy on 12/14/17.
 */
@Entity(value = "newRelicMetricNames", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("newRelicAppId"), @Field("newRelicConfigId")
  }, options = @IndexOptions(unique = true, name = "metricUniqueIdx"))
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewRelicMetricNames extends Base {
  @NotEmpty private String newRelicAppId;

  @NotEmpty private String newRelicConfigId;

  private List<NewRelicMetric> metrics;

  private List<WorkflowInfo> registeredWorkflows;

  private long lastUpdatedTime;

  @Builder
  @Data
  public static class WorkflowInfo {
    private String accountId;
    private String workflowId;
    private String appId;
    private String infraMappingId;
    private String envId;
  }
}
