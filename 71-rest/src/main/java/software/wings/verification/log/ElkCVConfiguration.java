package software.wings.verification.log;

import static software.wings.sm.states.ElkAnalysisState.DEFAULT_TIME_FIELD;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.stencils.DefaultValue;

@Data
@EqualsAndHashCode(callSuper = true)
public class ElkCVConfiguration extends LogsCVConfiguration {
  @Attributes(required = true, title = "Query Type") @DefaultValue("TERM") private ElkQueryType queryType;

  @Attributes(title = "Elastic search indices to search", required = true) @DefaultValue("_all") protected String index;

  @Attributes(required = true, title = "Host Name Field") @DefaultValue("hostname") protected String hostnameField;

  @Attributes(required = true, title = "Message Field") @DefaultValue("message") protected String messageField;

  @Attributes(required = true, title = "Timestamp Field")
  @DefaultValue(DEFAULT_TIME_FIELD)
  protected String timestampField;

  @Attributes(required = true, title = "Timestamp format")
  @DefaultValue("yyyy-MM-dd'T'HH:mm:ss.SSSX")
  private String timestampFormat;

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static final class ElkCVConfigurationYaml extends LogsCVConfigurationYaml {
    private String queryType;
    private String index;
    private String hostnameField;
    private String messageField;
    private String timestampField;
    private String timestampFormat;
  }
}
