package io.harness.cvng.core.beans.params.filterParams;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import java.util.List;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Data
@NoArgsConstructor
public abstract class AnalysisFilter {
  @QueryParam("filter") String filter;
  @QueryParam("healthSources") List<String> healthSourceIdentifiers;

  public boolean filterByHealthSourceIdentifiers() {
    return isNotEmpty(healthSourceIdentifiers);
  }

  public boolean filterByFilter() {
    return isNotEmpty(filter);
  }
}
