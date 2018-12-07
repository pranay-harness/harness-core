package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEc2ListTagsResponse extends AwsResponse {
  private Set<String> tags;

  @Builder
  public AwsEc2ListTagsResponse(ExecutionStatus executionStatus, String errorMessage, Set<String> tags) {
    super(executionStatus, errorMessage);
    this.tags = tags;
  }
}