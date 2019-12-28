package software.wings.service;

import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import io.harness.serializer.JsonUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Vaibhav Tulsyan
 * 04/Oct/2018
 */
public class ThirdPartyApiCallLogTest extends WingsBaseTest {
  private String title = UUID.randomUUID().toString();
  private String stateExecutionId = UUID.randomUUID().toString();
  private String accountId = UUID.randomUUID().toString();
  private String delegateId = UUID.randomUUID().toString();
  private String delegateTaskId = UUID.randomUUID().toString();
  private List<ThirdPartyApiCallField> request = new ArrayList<>();

  private String responseText = UUID.randomUUID().toString();

  private ThirdPartyApiCallLog failedCallLog, successfulCallLog, mixedCallLog;

  @Before
  public void setUp() throws Exception {
    failedCallLog = ThirdPartyApiCallLog.builder()
                        .title(title)
                        .stateExecutionId(stateExecutionId)
                        .accountId(accountId)
                        .delegateId(delegateId)
                        .delegateTaskId(delegateTaskId)
                        .request(request)
                        .requestTimeStamp(0)
                        .responseTimeStamp(1)
                        .build();
    failedCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, responseText, FieldType.TEXT);

    successfulCallLog = ThirdPartyApiCallLog.builder()
                            .title(title)
                            .stateExecutionId(stateExecutionId)
                            .accountId(accountId)
                            .delegateId(delegateId)
                            .delegateTaskId(delegateTaskId)
                            .request(request)
                            .requestTimeStamp(0)
                            .responseTimeStamp(1)
                            .build();
    successfulCallLog.addFieldToResponse(HttpStatus.SC_OK, responseText, FieldType.TEXT);

    mixedCallLog = ThirdPartyApiCallLog.builder()
                       .title(title)
                       .stateExecutionId(stateExecutionId)
                       .accountId(accountId)
                       .delegateId(delegateId)
                       .delegateTaskId(delegateTaskId)
                       .request(request)
                       .requestTimeStamp(0)
                       .responseTimeStamp(1)
                       .build();
    mixedCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, responseText, FieldType.TEXT);
    mixedCallLog.addFieldToResponse(HttpStatus.SC_OK, responseText, FieldType.TEXT);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testStatusTag() throws IOException {
    String failedCallLogJson = JsonUtils.asJson(failedCallLog);
    ThirdPartyApiCallLog obj = JsonUtils.asObject(failedCallLogJson, ThirdPartyApiCallLog.class);
    assertThat(obj.getStatus()).isEqualTo(ExecutionStatus.FAILED);

    String successfulCallLogJson = JsonUtils.asJson(successfulCallLog);
    obj = JsonUtils.asObject(successfulCallLogJson, ThirdPartyApiCallLog.class);
    assertThat(obj.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);

    String mixedCallLogJson = JsonUtils.asJson(mixedCallLog);
    obj = JsonUtils.asObject(mixedCallLogJson, ThirdPartyApiCallLog.class);
    assertThat(obj.getStatus()).isEqualTo(ExecutionStatus.FAILED);
  }
}
