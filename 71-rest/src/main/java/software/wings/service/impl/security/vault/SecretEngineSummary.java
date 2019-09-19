package software.wings.service.impl.security.vault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * @author marklu on 9/13/19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@ToString
public class SecretEngineSummary {
  private String name;
  private String description;
  private String type;
  private Integer version;
}
