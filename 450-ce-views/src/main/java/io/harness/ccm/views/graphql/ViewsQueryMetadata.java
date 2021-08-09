package io.harness.ccm.views.graphql;

import com.healthmarketscience.sqlbuilder.SelectQuery;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ViewsQueryMetadata {
  SelectQuery query;
  List<QLCEViewFieldInput> fields;
}
