package io.harness.delegate.task.spotinst.response;

import com.amazonaws.services.ec2.model.Instance;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpotInstListElastigroupInstancesResponse implements SpotInstTaskResponse {
  private String elastigroupId;
  private List<Instance> elastigroupInstances;
}
