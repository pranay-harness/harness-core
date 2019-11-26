package software.wings.verification.dynatrace;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
@EqualsAndHashCode(callSuper = false)
public class DynaTraceCVServiceConfiguration extends CVConfiguration {
  /**
   * The type Yaml.
   */
  @Data
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class DynaTraceCVConfigurationYaml extends CVConfigurationYaml {}
}
