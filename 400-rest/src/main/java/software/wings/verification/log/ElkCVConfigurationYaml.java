package software.wings.verification.log;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ElkCVConfigurationYaml extends LogsCVConfigurationYaml {
  private String index;
  private String hostnameField;
  private String messageField;
  private String timestampField;
  private String timestampFormat;
}
