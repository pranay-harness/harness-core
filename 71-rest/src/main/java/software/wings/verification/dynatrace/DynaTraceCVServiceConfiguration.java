package software.wings.verification.dynatrace;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.verification.CVConfiguration;

/**
 * Created by Pranjal on 10/16/2018
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DynaTraceCVServiceConfiguration extends CVConfiguration {
  @Attributes(required = true, title = "Service Methods") private String serviceMethods;

  /**
   * The type Yaml.
   */
  @Data
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class DynaTraceCVConfigurationYaml extends CVConfigurationYaml {
    private String serviceMethods;
  }
}
