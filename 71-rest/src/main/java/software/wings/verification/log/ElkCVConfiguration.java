package software.wings.verification.log;

import static software.wings.sm.states.ElkAnalysisState.DEFAULT_TIME_FIELD;

import com.github.reinert.jjschema.Attributes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.stencils.DefaultValue;

@Data
@EqualsAndHashCode(callSuper = true)
public class ElkCVConfiguration extends AbstractLogsCVConfiguration {
  @Attributes(required = true, title = "Query Type") @DefaultValue("TERM") private String queryType;

  @Attributes(title = "Elastic search indices to search", required = true)
  @DefaultValue("_all")
  protected String indices;

  @Attributes(required = true, title = "Message Field") @DefaultValue("message") protected String messageField;

  @Attributes(required = true, title = "Timestamp Field")
  @DefaultValue(DEFAULT_TIME_FIELD)
  protected String timestampField;

  @Attributes(required = true, title = "Timestamp format")
  @DefaultValue("yyyy-MM-dd'T'HH:mm:ss.SSSX")
  private String timestampFormat;
}
