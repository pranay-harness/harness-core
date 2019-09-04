package software.wings.service.impl.security.vault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * @author marklu on 9/3/19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@ToString
public class SysMount {
  private String accessor;
  private String description;
  private String type;
  private SysMountOptions options;
}
