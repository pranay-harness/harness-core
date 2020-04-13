package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.harness.data.validator.Trimmed;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Transient;

@Data
@Builder
@FieldNameConstants(innerTypeName = "HelmChartConfigKeys")
public class HelmChartConfig {
  @Trimmed private String connectorId;
  @Trimmed private String chartName;
  @Trimmed private String chartVersion;
  @Trimmed private String chartUrl;
  @Transient @JsonInclude(Include.NON_EMPTY) private String connectorName;
  private String basePath;

  public String getBasePath() {
    return basePath == null ? "" : basePath;
  }
}
