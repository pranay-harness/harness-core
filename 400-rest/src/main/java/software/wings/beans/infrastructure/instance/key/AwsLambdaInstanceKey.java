package software.wings.beans.infrastructure.instance.key;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsLambdaInstanceKey extends ServerlessInstanceKey {
  private String functionName;
  private String functionVersion;

  @Builder
  public AwsLambdaInstanceKey(String functionName, String functionVersion) {
    this.functionName = functionName;
    this.functionVersion = functionVersion;
  }
}
