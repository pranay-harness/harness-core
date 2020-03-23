package io.harness.event.timeseries.processor;

import static io.harness.rule.OwnerRule.RAMA;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.service.impl.event.timeseries.TimeSeriesEventInfo;
import software.wings.utils.WingsTestConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rktummala
 */
public class DeploymentEventProcessorTest extends WingsBaseTest {
  @Mock TimeScaleDBService timeScaleService;
  @Inject @InjectMocks DeploymentEventProcessor deploymentEventProcessor;

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldListExecutions() throws SQLException {
    Map<String, String> stringData = new HashMap<>();
    stringData.put(EventProcessor.ACCOUNTID, ACCOUNT_ID);
    stringData.put(EventProcessor.EXECUTIONID, WORKFLOW_EXECUTION_ID);
    stringData.put(EventProcessor.APPID, APP_ID);
    stringData.put(EventProcessor.TRIGGERED_BY, WingsTestConstants.USER_ID);
    stringData.put(EventProcessor.TRIGGER_ID, WingsTestConstants.TRIGGER_ID);
    stringData.put(EventProcessor.STATUS, ExecutionStatus.SUCCESS.name());
    stringData.put(EventProcessor.PARENT_EXECUTION, WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID);
    stringData.put(EventProcessor.STAGENAME, WingsTestConstants.WORKFLOW_NAME);
    stringData.put(EventProcessor.PIPELINE, WingsTestConstants.PIPELINE_NAME);

    Map<String, Long> longData = new HashMap<>();
    longData.put(EventProcessor.ROLLBACK_DURATION, 123L);
    longData.put(EventProcessor.STARTTIME, 233L);
    longData.put(EventProcessor.ENDTIME, 244L);
    longData.put(EventProcessor.DURATION, 33L);

    Map<String, Integer> integerData = new HashMap<>();
    integerData.put(EventProcessor.INSTANCES_DEPLOYED, 1);

    Map<String, List<String>> listData = new HashMap<>();
    listData.put(EventProcessor.SERVICE_LIST, asList(SERVICE_ID));
    listData.put(EventProcessor.WORKFLOW_LIST, asList(WORKFLOW_ID));
    listData.put(EventProcessor.CLOUD_PROVIDER_LIST, asList(WingsTestConstants.COMPUTE_PROVIDER_ID));
    listData.put(EventProcessor.ENV_LIST, asList(ENV_ID));
    listData.put(EventProcessor.ARTIFACT_LIST, asList(ARTIFACT_ID));
    listData.put(EventProcessor.ENVTYPES, asList(EnvironmentType.PROD.name()));

    Map<String, Object> objectData = new HashMap<>();
    Map<String, String> tags = new HashMap<>();
    tags.put("env", "QA");
    tags.put("description", "this is a really really long description");
    objectData.put(EventProcessor.TAGS, tags);

    TimeSeriesEventInfo timeSeriesEventInfo = TimeSeriesEventInfo.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .longData(longData)
                                                  .integerData(integerData)
                                                  .stringData(stringData)
                                                  .listData(listData)
                                                  .data(objectData)
                                                  .build();

    when(timeScaleService.isValid()).thenReturn(true);
    Connection dbConnection = mock(Connection.class);
    when(timeScaleService.getDBConnection()).thenReturn(dbConnection);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    when(dbConnection.prepareStatement(anyString())).thenReturn(preparedStatement);
    deploymentEventProcessor.processEvent(timeSeriesEventInfo);
    verify(preparedStatement).execute();
  }
}
