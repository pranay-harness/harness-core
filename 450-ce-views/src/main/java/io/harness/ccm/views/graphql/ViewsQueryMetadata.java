package io.harness.ccm.views.graphql;

import com.healthmarketscience.sqlbuilder.SelectQuery;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ViewsQueryMetadata {
  SelectQuery query;
  List<QLCEViewFieldInput> fields;
}
