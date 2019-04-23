package software.wings.verification.log;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by Pranjal on 03/29/2019
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BugsnagCVConfiguration extends LogsCVConfiguration {
  @Attributes(required = true, title = "Organization") private String orgId;

  @Attributes(required = true, title = "Project") protected String projectId;

  @Attributes(title = "Release Stage") protected String releaseStage;

  @Attributes(required = true, title = "Browser Application") protected boolean browserApplication;

  /**
   * The type Yaml.
   */
  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static final class BugsnagCVConfigurationYaml extends LogsCVConfigurationYaml {
    private String orgName;
    private String projectName;
    private String releaseStage;
    private boolean browserApplication;
  }
}
