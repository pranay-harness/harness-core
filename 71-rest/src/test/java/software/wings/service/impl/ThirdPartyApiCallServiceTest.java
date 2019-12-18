package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.resources.ActivityResource;
import software.wings.resources.DelegateAgentResource;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rsingh on 9/13/18.
 */
public class ThirdPartyApiCallServiceTest extends WingsBaseTest {
  private String accountId;
  private String stateExecutionId;
  private String delegateId;
  private String appId;
  @Inject private DelegateAgentResource delegateResource;
  @Inject private ActivityResource activityResource;

  @Before
  public void setup() throws IOException, NoSuchFieldException, IllegalAccessException {
    accountId = generateUuid();
    appId = generateUuid();
    stateExecutionId = generateUuid();
    delegateId = delegateId;
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testSaveAndGetApiCallLogs() throws Exception {
    int numOfApiCallLogs = 10;
    List<ThirdPartyApiCallLog> apiCallLogs = new ArrayList<>();
    for (int i = 0; i < numOfApiCallLogs; i++) {
      apiCallLogs.add(ThirdPartyApiCallLog.builder()
                          .stateExecutionId(stateExecutionId)
                          .accountId(accountId)
                          .requestTimeStamp(System.currentTimeMillis())
                          .responseTimeStamp(System.currentTimeMillis())
                          .request(Lists.newArrayList(ThirdPartyApiCallField.builder()
                                                          .type(FieldType.TEXT)
                                                          .value(generateUuid())
                                                          .name(generateUuid())
                                                          .build(),
                              ThirdPartyApiCallField.builder()
                                  .type(FieldType.TEXT)
                                  .value(generateUuid())
                                  .name(generateUuid())
                                  .build()))
                          .response(Lists.newArrayList(ThirdPartyApiCallField.builder()
                                                           .type(FieldType.TEXT)
                                                           .value(generateUuid())
                                                           .name(generateUuid())
                                                           .build(),
                              ThirdPartyApiCallField.builder()
                                  .type(FieldType.TEXT)
                                  .value(generateUuid())
                                  .name(generateUuid())
                                  .build()))
                          .createdAt(i + 1)
                          .build());
    }
    delegateResource.saveApiCallLogs(delegateId, accountId, apiCallLogs);

    RestResponse<List<ThirdPartyApiCallLog>> restResponse =
        activityResource.listLogs(appId, stateExecutionId, 0, 0, aPageRequest().build());
    List<ThirdPartyApiCallLog> savedApiCallLogs = restResponse.getResource();
    assertThat(savedApiCallLogs).hasSize(numOfApiCallLogs);
    for (int i = 0; i < numOfApiCallLogs; i++) {
      assertThat(savedApiCallLogs.get(i).getCreatedAt()).isEqualTo(numOfApiCallLogs - i);
      assertThat(savedApiCallLogs.get(i).getAccountId()).isEqualTo(accountId);
      assertThat(savedApiCallLogs.get(i).getStateExecutionId()).isEqualTo(stateExecutionId);
      assertThat(savedApiCallLogs.get(i).getRequestTimeStamp())
          .isEqualTo(apiCallLogs.get(numOfApiCallLogs - i - 1).getRequestTimeStamp());
      assertThat(savedApiCallLogs.get(i).getResponseTimeStamp())
          .isEqualTo(apiCallLogs.get(numOfApiCallLogs - i - 1).getResponseTimeStamp());
      assertThat(savedApiCallLogs.get(i).getRequest())
          .isEqualTo(apiCallLogs.get(numOfApiCallLogs - i - 1).getRequest());
      assertThat(savedApiCallLogs.get(i).getResponse())
          .isEqualTo(apiCallLogs.get(numOfApiCallLogs - i - 1).getResponse());
    }
  }
}
