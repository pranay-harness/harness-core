package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsEcrGetImageUrlResponse extends AwsResponse {
  String ecrImageUrl;

  @Builder
  public AwsEcrGetImageUrlResponse(ExecutionStatus executionStatus, String errorMessage, String ecrImageUrl) {
    super(executionStatus, errorMessage);
    this.ecrImageUrl = ecrImageUrl;
  }
}