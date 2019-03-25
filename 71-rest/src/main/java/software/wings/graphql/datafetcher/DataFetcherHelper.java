package software.wings.graphql.datafetcher;

import static software.wings.graphql.datafetcher.SchemaFieldsEnum.WORKFLOW;
import static software.wings.graphql.datafetcher.SchemaFieldsEnum.WORKFLOW_EXECUTION_STATUS;
import static software.wings.graphql.datafetcher.SchemaFieldsEnum.WORKFLOW_LIST;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import graphql.schema.DataFetcher;

import java.util.Map;

/**
 * This class is like a aggregator of all the
 * field/endpoints(operations) that will be exposed to customers.
 *
 * So, if someone wants to add new field/operation
 * they can just add an endry in <code>SchemaFieldsEnum</code>
 * and add the corresponding <code>DataFetcher</code> in this class
 */
public class DataFetcherHelper {
  @Inject private WorkflowDataFetcher workflowDataFetcher;

  /**
   * Later, we should have TEST to make sure a fieldName is only used once
   * otherwise it may be overridden.
   * @return
   */
  public Map<String, DataFetcher<?>> getDataFetcherMap() {
    return ImmutableMap.of(WORKFLOW.getFieldName(), workflowDataFetcher.getWorkFlow(), WORKFLOW_LIST.getFieldName(),
        workflowDataFetcher.getWorkFlows(), WORKFLOW_EXECUTION_STATUS.getFieldName(),
        workflowDataFetcher.getWorkFlowExecutionStatus());
  }
}
