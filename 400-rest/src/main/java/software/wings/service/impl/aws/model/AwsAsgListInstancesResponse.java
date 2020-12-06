package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;

import com.amazonaws.services.ec2.model.Instance;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsAsgListInstancesResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private String asgName;
  private List<Instance> instances;
}
