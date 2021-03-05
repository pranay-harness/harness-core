package software.wings.beans;

import io.harness.yaml.BaseYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class LambdaSpecificationsYaml extends BaseYaml {
  private String runtime;
  private Integer memorySize = 128;
  private Integer timeout = 3;
  private String functionName;
  private String handler;

  @Builder
  public LambdaSpecificationsYaml(
      String runtime, Integer memorySize, Integer timeout, String functionName, String handler) {
    this.runtime = runtime;
    this.memorySize = memorySize;
    this.timeout = timeout;
    this.functionName = functionName;
    this.handler = handler;
  }
}
