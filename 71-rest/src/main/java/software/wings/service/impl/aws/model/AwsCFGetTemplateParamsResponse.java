package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCFGetTemplateParamsResponse extends AwsResponse {
  private List<AwsCFTemplateParamsData> parameters;

  @Builder
  public AwsCFGetTemplateParamsResponse(
      ExecutionStatus executionStatus, String errorMessage, List<AwsCFTemplateParamsData> parameters) {
    super(executionStatus, errorMessage);
    this.parameters = parameters;
  }
}