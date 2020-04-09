package io.harness.delegate.task.spotinst.response;

import com.amazonaws.services.ec2.model.Instance;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SpotinstTrafficShiftAlbDeployResponse implements SpotInstTaskResponse {
  private List<Instance> ec2InstancesAdded;
  private List<Instance> ec2InstancesExisting;
}