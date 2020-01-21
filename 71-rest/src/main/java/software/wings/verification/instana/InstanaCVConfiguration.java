package software.wings.verification.instana;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import software.wings.verification.CVConfiguration;

import java.util.ArrayList;
import java.util.List;
@FieldNameConstants(innerTypeName = "InstanaCVConfigurationKeys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class InstanaCVConfiguration extends CVConfiguration {
  private String query;
  private List<String> metrics;

  @Override
  public CVConfiguration deepCopy() {
    InstanaCVConfiguration clonedConfig = new InstanaCVConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setQuery(this.getQuery());
    clonedConfig.setMetrics(new ArrayList<>(this.getMetrics()));
    return clonedConfig;
  }

  /**
   * The type Yaml.
   */
  @Data
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class InstanaCVConfigurationYaml extends CVConfigurationYaml {
    private String query;
    private List<String> metrics;
  }
}
