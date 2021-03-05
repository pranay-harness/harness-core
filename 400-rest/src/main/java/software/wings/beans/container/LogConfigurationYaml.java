package software.wings.beans.container;

import io.harness.yaml.BaseYaml;

import software.wings.beans.NameValuePairYaml;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class LogConfigurationYaml extends BaseYaml {
  private String logDriver;
  private List<NameValuePairYaml> options;

  @Builder
  public LogConfigurationYaml(String logDriver, List<NameValuePairYaml> options) {
    this.logDriver = logDriver;
    this.options = options;
  }
}
