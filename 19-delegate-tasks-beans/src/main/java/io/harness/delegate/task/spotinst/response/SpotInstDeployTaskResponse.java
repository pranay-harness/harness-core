package io.harness.delegate.task.spotinst.response;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.spotinst.model.ElastiGroup;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SpotInstDeployTaskResponse implements SpotInstTaskResponse {
  private List<Instance> ec2InstancesAdded;
  private ElastiGroup newElastiGroupBeforeDeploy;
  private ElastiGroup newElastiGroupAfterDeploy;
  private ElastiGroup oldElastiGroupBeforeDeploy;
  private ElastiGroup oldElastiGroupAfterDeploy;
}