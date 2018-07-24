package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsLambdaFunctionParams {
  private String key;
  private String bucket;
  private String functionName;
  private String handler;
  private String runtime;
  private Integer memory;
  private Integer timeout;
}