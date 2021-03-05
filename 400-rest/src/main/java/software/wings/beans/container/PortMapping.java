package software.wings.beans.container;

import io.harness.yaml.BaseYaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class PortMapping {
  @Attributes(title = "Container port") private Integer containerPort;
  @Attributes(title = "Host port") private Integer hostPort;
}
