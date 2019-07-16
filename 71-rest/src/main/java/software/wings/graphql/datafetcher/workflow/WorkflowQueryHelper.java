package software.wings.graphql.datafetcher.workflow;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Workflow;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.workflow.QLWorkflowFilter;

import java.util.List;

/**
 * @author rktummala on 07/12/19
 */
@Singleton
public class WorkflowQueryHelper {
  @Inject protected DataFetcherUtils utils;

  public void setQuery(List<QLWorkflowFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<Workflow>> field;

      if (filter.getApplication() != null) {
        field = query.field("appId");
        QLIdFilter applicationFilter = filter.getApplication();
        utils.setIdFilter(field, applicationFilter);
      }

      if (filter.getWorkflow() != null) {
        field = query.field("_id");
        QLIdFilter workflowFilter = filter.getWorkflow();
        utils.setIdFilter(field, workflowFilter);
      }
    });
  }
}
