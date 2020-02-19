package software.wings.service.impl.stackdriver;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.common.VerificationConstants.CV_DATA_COLLECTION_INTERVAL_IN_MINUTE;
import static software.wings.service.impl.stackdriver.StackDriverDelegateServiceImpl.MAX_LOGS_PER_MINUTE;

import com.google.api.services.logging.v2.Logging;
import com.google.api.services.logging.v2.model.ListLogEntriesRequest;
import com.google.api.services.logging.v2.model.ListLogEntriesResponse;
import com.google.api.services.logging.v2.model.LogEntry;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.delegatetasks.DelegateCVActivityLogService;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.service.impl.GcpHelperService;
import software.wings.service.intfc.stackdriver.StackDriverDelegateService;

import java.util.List;

public class StackDriverDelegateServiceImplTest extends WingsBaseTest {
  @Inject private StackDriverDelegateService stackDriverDelegateService;
  @Mock private GcpHelperService gcpHelperService;
  @Mock private DelegateCVActivityLogService delegateCVActivityLogService;

  private String accountId;
  private StackDriverLogDataCollectionInfo dataCollectionInfo;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setupTests() throws Exception {
    accountId = generateUuid();
    FieldUtils.writeField(stackDriverDelegateService, "gcpHelperService", gcpHelperService, true);
    FieldUtils.writeField(
        stackDriverDelegateService, "delegateCVActivityLogService", delegateCVActivityLogService, true);
    Logging logging = mock(Logging.class);
    Logging.Entries entries = mock(Logging.Entries.class);
    when(logging.entries()).thenReturn(entries);
    Logging.Entries.List list = mock(Logging.Entries.List.class);
    when(entries.list(any(ListLogEntriesRequest.class))).thenReturn(list);

    ListLogEntriesResponse listLogEntriesResponse = new ListLogEntriesResponse();
    listLogEntriesResponse.setNextPageToken(generateUuid());
    listLogEntriesResponse.setEntries(Lists.newArrayList(new LogEntry(), new LogEntry()));

    when(list.execute()).thenReturn(listLogEntriesResponse);
    when(gcpHelperService.getLoggingResource(any(GcpConfig.class), anyList(), anyString())).thenReturn(logging);
    DelegateCVActivityLogService.Logger logger = mock(DelegateCVActivityLogService.Logger.class);
    when(delegateCVActivityLogService.getLogger(
             anyString(), anyString(), anyLong(), anyString(), anyString(), anyLong(), anyLong()))
        .thenReturn(logger);

    dataCollectionInfo = StackDriverLogDataCollectionInfo.builder()
                             .gcpConfig(GcpConfig.builder()
                                            .accountId(accountId)
                                            .serviceAccountKeyFileContent("{\"project_id\":\"test\"}".toCharArray())
                                            .build())
                             .query(generateUuid())
                             .hostnameField(generateUuid())
                             .hosts(Sets.newHashSet(generateUuid()))
                             .stateExecutionId(generateUuid())
                             .build();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testFetchLogs_whenLogsLimitReachedForWorkflow() {
    assertThatThrownBy(()
                           -> stackDriverDelegateService.fetchLogs(
                               dataCollectionInfo, System.currentTimeMillis(), System.currentTimeMillis(), false, true))
        .isInstanceOf(DataCollectionException.class)
        .hasMessage("Limit of " + MAX_LOGS_PER_MINUTE + " logs per minute reached. Please refine your query.");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testFetchLogs_whenLogsLimitReachedForServiceGuard() {
    final List<LogEntry> logEntries = stackDriverDelegateService.fetchLogs(
        dataCollectionInfo, System.currentTimeMillis(), System.currentTimeMillis(), true, true);
    assertThat(logEntries.size()).isEqualTo(MAX_LOGS_PER_MINUTE * CV_DATA_COLLECTION_INTERVAL_IN_MINUTE);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testFetchLogs_whenNoNextPage() {
    final List<LogEntry> logEntries = stackDriverDelegateService.fetchLogs(
        dataCollectionInfo, System.currentTimeMillis(), System.currentTimeMillis(), false, false);
    assertThat(logEntries.size()).isEqualTo(2);
  }
}