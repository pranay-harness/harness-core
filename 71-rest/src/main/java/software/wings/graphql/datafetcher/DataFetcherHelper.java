package software.wings.graphql.datafetcher;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import graphql.schema.DataFetcher;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.Map;

/**
 * This class is like a aggregator of all the
 * field/endpoints(operations) that will be exposed to customers.
 *
 * So, if someone wants to add new field/operation
 * they can just add an endry in <code>QueryOperationsEnum</code>
 * and add the corresponding <code>DataFetcher</code> in this class
 */
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataFetcherHelper {
  @Inject @Named("workflowDataFetcher") AbstractDataFetcher workflowDataFetcher;

  @Inject @Named("workflowExecutionDataFetcher") AbstractDataFetcher workflowExecutionDataFetcher;

  @Inject @Named("artifactDataFetcher") AbstractDataFetcher artifactDataFetcher;

  @Inject @Named("applicationDataFetcher") AbstractDataFetcher applicationDataFetcher;

  /**
   * Later, we should have TEST to make sure a fieldName is only used once
   * otherwise it may be overridden.
   * @return
   */
  public Map<String, DataFetcher<?>> getDataFetcherMap() {
    return ImmutableMap.<String, DataFetcher<?>>builder()
        .putAll(workflowDataFetcher.getOperationToDataFetcherMap())
        .putAll(workflowExecutionDataFetcher.getOperationToDataFetcherMap())
        .putAll(artifactDataFetcher.getOperationToDataFetcherMap())
        .putAll(applicationDataFetcher.getOperationToDataFetcherMap())
        .build();
  }
}
