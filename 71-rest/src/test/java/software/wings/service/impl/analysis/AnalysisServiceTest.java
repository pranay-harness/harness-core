package software.wings.service.impl.analysis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.api.JiraExecutionData;
import software.wings.beans.jira.JiraTaskParameters;
import software.wings.delegatetasks.jira.JiraAction;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.impl.analysis.AnalysisServiceImpl.CLUSTER_TYPE;
import software.wings.service.impl.analysis.CVFeedbackRecord.CVFeedbackRecordKeys;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;

public class AnalysisServiceTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Inject AnalysisService analysisService;
  @Mock JiraHelperService jiraHelperService;
  String accountId;
  String appId;
  String serviceId;
  String envId;
  CVConfiguration cvConfiguration;
  String jiraConfigId;

  @Before
  public void setupTests() throws Exception {
    accountId = generateUuid();
    serviceId = generateUuid();
    appId = generateUuid();
    envId = generateUuid();
    jiraConfigId = generateUuid();
    cvConfiguration = new CVConfiguration();
    cvConfiguration.setStateType(StateType.ELK);
    cvConfiguration.setServiceId(serviceId);
    cvConfiguration.setEnvId(envId);
    cvConfiguration.setAccountId(accountId);

    cvConfiguration.setUuid(generateUuid());
    wingsPersistence.save(cvConfiguration);
    FieldUtils.writeField(analysisService, "jiraHelperService", jiraHelperService, true);
    JiraExecutionData executionData =
        JiraExecutionData.builder().executionStatus(ExecutionStatus.SUCCESS).issueUrl("tempJiraUrl").build();
    when(jiraHelperService.createJira(anyString(), anyString(), anyString(), anyObject())).thenReturn(executionData);
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Category(UnitTests.class)
  public void testCreateJira() {
    CVCollaborationProviderParameters cvJiraParameters = CVCollaborationProviderParameters.builder().build();
    cvJiraParameters.setCollaborationProviderConfigId(jiraConfigId);
    JiraTaskParameters taskParameters = JiraTaskParameters.builder()
                                            .jiraAction(JiraAction.CREATE_TICKET)
                                            .project("LE")
                                            .priority("P0")
                                            .description("This is a log message")
                                            .appId(appId)
                                            .build();

    CVFeedbackRecord feedbackRecord = CVFeedbackRecord.builder()
                                          .envId(envId)
                                          .cvConfigId(cvConfiguration.getUuid())
                                          .logMessage("This is a log message")
                                          .clusterType(CLUSTER_TYPE.UNKNOWN)
                                          .clusterLabel(2)
                                          .build();
    cvJiraParameters.setCvFeedbackRecord(feedbackRecord);
    cvJiraParameters.setJiraTaskParameters(taskParameters);

    String jiraLink = analysisService.createCollaborationFeedbackTicket(
        accountId, appId, cvConfiguration.getUuid(), null, cvJiraParameters);

    CVFeedbackRecord record = wingsPersistence.createQuery(CVFeedbackRecord.class)
                                  .filter(CVFeedbackRecordKeys.envId, envId)
                                  .filter(CVFeedbackRecordKeys.serviceId, serviceId)
                                  .get();

    assertEquals(FeedbackAction.ADD_TO_BASELINE, record.getActionTaken());
    assertEquals("This is a log message", record.getLogMessage());
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testCreateJiraMissingTaskParams() {
    CVCollaborationProviderParameters cvJiraParameters = CVCollaborationProviderParameters.builder().build();
    cvJiraParameters.setCollaborationProviderConfigId(null);

    CVFeedbackRecord feedbackRecord = CVFeedbackRecord.builder()
                                          .envId(envId)
                                          .cvConfigId(cvConfiguration.getUuid())
                                          .logMessage("This is a log message")
                                          .clusterType(CLUSTER_TYPE.UNKNOWN)
                                          .clusterLabel(2)
                                          .build();
    JiraTaskParameters taskParameters = JiraTaskParameters.builder()
                                            .jiraAction(JiraAction.CREATE_TICKET)
                                            .project("LE")
                                            .priority("P0")
                                            .description("This is a log message")
                                            .appId(appId)
                                            .build();
    cvJiraParameters.setCvFeedbackRecord(feedbackRecord);
    cvJiraParameters.setJiraTaskParameters(taskParameters);

    String jiraLink = analysisService.createCollaborationFeedbackTicket(
        accountId, appId, cvConfiguration.getUuid(), null, cvJiraParameters);
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testCreateJiraMissingJiraConfigId() {
    CVCollaborationProviderParameters cvJiraParameters = CVCollaborationProviderParameters.builder().build();

    CVFeedbackRecord feedbackRecord = CVFeedbackRecord.builder()
                                          .envId(envId)
                                          .cvConfigId(cvConfiguration.getUuid())
                                          .logMessage("This is a log message")
                                          .clusterType(CLUSTER_TYPE.UNKNOWN)
                                          .clusterLabel(2)
                                          .build();
    cvJiraParameters.setCvFeedbackRecord(feedbackRecord);
    cvJiraParameters.setJiraTaskParameters(null);

    String jiraLink = analysisService.createCollaborationFeedbackTicket(
        accountId, appId, cvConfiguration.getUuid(), null, cvJiraParameters);
  }
}
