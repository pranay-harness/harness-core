package software.wings.graphql.datafetcher.pipeline;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Pipeline;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineFilter;

import java.util.List;

/**
 * @author rktummala on 07/12/19
 */
@Singleton
public class PipelineQueryHelper {
  @Inject protected DataFetcherUtils utils;

  public void setQuery(List<QLPipelineFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<Pipeline>> field;

      if (filter.getApplication() != null) {
        field = query.field("appId");
        QLIdFilter applicationFilter = filter.getApplication();
        utils.setIdFilter(field, applicationFilter);
      }

      if (filter.getPipeline() != null) {
        field = query.field("_id");
        QLIdFilter pipelineFilter = filter.getPipeline();
        utils.setIdFilter(field, pipelineFilter);
      }
    });
  }
}
