package software.wings.verification.log;

import com.github.reinert.jjschema.Attributes;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.json.JSONException;
import org.json.JSONObject;
import software.wings.stencils.DefaultValue;
import software.wings.verification.CVConfiguration;

@Data
@EqualsAndHashCode(callSuper = true)
public class LogsCVConfiguration extends CVConfiguration {
  @Attributes(title = "Search Keywords", required = true) @DefaultValue("*exception*") protected String query;
  @Attributes(required = true, title = "Is Formatted Query") @DefaultValue("false") private boolean formattedQuery;

  private long baselineStartMinute = -1;
  private long baselineEndMinute = -1;

  public void setQuery(String query) {
    try {
      new JSONObject(query);
      if (!formattedQuery) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "JSON Query passed. Please select Formatted option");
      }
    } catch (JSONException ex) {
      if (formattedQuery) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Invalid JSON Query Passed");
      }
    }
    this.query = query.trim();
  }
}
