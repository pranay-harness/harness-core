package software.wings.verification.log;

import com.github.reinert.jjschema.Attributes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.stencils.DefaultValue;
import software.wings.verification.CVConfiguration;

@Data
@EqualsAndHashCode(callSuper = true)
public class LogsCVConfiguration extends CVConfiguration {
  @Attributes(required = true, title = "Is Formatted Query") @DefaultValue("false") private boolean formattedQuery;
  @Attributes(title = "Search Keywords", required = true) @DefaultValue("*exception*") protected String query;

  private long baselineStartMinute = -1;
  private long baselineEndMinute = -1;

  public void setQuery(String query) {
    this.query = query.trim();
  }
}
