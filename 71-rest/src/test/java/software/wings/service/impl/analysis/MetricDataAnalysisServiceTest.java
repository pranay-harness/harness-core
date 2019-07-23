package software.wings.service.impl.analysis;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.StateType;

import java.util.Arrays;

/**
 * @author Praveen
 *
 */
public class MetricDataAnalysisServiceTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Inject MetricDataAnalysisService metricDataAnalysisService;

  String appId, workflowId, serviceId, infraMappingId;

  @Before
  public void setup() {
    appId = generateUuid();
    serviceId = generateUuid();
    workflowId = generateUuid();
    infraMappingId = generateUuid();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetLastSuccessfulWorkflowWithData() {
    String execId = generateUuid();
    WorkflowExecution execution = WorkflowExecution.builder()
                                      .appId(appId)
                                      .workflowId(workflowId)
                                      .infraMappingIds(Arrays.asList(infraMappingId))
                                      .serviceIds(Arrays.asList(serviceId))
                                      .status(SUCCESS)
                                      .uuid(execId)
                                      .build();

    wingsPersistence.save(execution);

    NewRelicMetricDataRecord record = NewRelicMetricDataRecord.builder()
                                          .stateType(StateType.NEW_RELIC)
                                          .appId(appId)
                                          .workflowId(workflowId)
                                          .workflowExecutionId(execId)
                                          .serviceId(serviceId)
                                          .build();
    wingsPersistence.save(record);

    String lastId = metricDataAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, appId, workflowId, serviceId, infraMappingId);

    assertNotNull(lastId);
    assertEquals(execId, lastId);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetLastSuccessfulWorkflowWithDataDifferentInfra() {
    String execId = generateUuid();
    String newInfra = generateUuid();
    WorkflowExecution execution = WorkflowExecution.builder()
                                      .appId(appId)
                                      .workflowId(workflowId)
                                      .infraMappingIds(Arrays.asList(newInfra))
                                      .serviceIds(Arrays.asList(serviceId))
                                      .status(SUCCESS)
                                      .uuid(execId)
                                      .build();

    wingsPersistence.save(execution);

    NewRelicMetricDataRecord record = NewRelicMetricDataRecord.builder()
                                          .stateType(StateType.NEW_RELIC)
                                          .appId(appId)
                                          .workflowId(workflowId)
                                          .workflowExecutionId(execId)
                                          .serviceId(serviceId)
                                          .build();
    wingsPersistence.save(record);

    String lastId = metricDataAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
        StateType.NEW_RELIC, appId, workflowId, serviceId, infraMappingId);

    assertNull(lastId);
  }
}
