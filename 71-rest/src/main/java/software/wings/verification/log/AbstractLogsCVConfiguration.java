package software.wings.verification.log;

import com.github.reinert.jjschema.Attributes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.stencils.DefaultValue;
import software.wings.verification.CVConfiguration;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractLogsCVConfiguration extends CVConfiguration {
  @Attributes(title = "Search Keywords", required = true) @DefaultValue("*exception*") protected String query;
  @Attributes(required = true, title = "Is Formatted Query") @DefaultValue("false") private boolean formattedQuery;
}
