package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.RestResponse;
import software.wings.resources.ActivityResource;
import software.wings.resources.DelegateResource;
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
  @Inject private DelegateResource delegateResource;
  @Inject private ActivityResource activityResource;

  @Before
  public void setup() throws IOException, NoSuchFieldException, IllegalAccessException {
    accountId = generateUuid();
    appId = generateUuid();
    stateExecutionId = generateUuid();
    delegateId = delegateId;
  }

  @Test
  public void testSaveAndGetApiCallLogs() throws Exception {
    int numOfApiCallLogs = 10;
    List<ThirdPartyApiCallLog> apiCallLogs = new ArrayList<>();
    for (int i = 0; i < numOfApiCallLogs; i++) {
      apiCallLogs.add(ThirdPartyApiCallLog.builder()
                          .stateExecutionId(stateExecutionId)
                          .accountId(accountId)
                          .appId(appId)
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
        activityResource.listLogs(appId, stateExecutionId, aPageRequest().build());
    List<ThirdPartyApiCallLog> savedApiCallLogs = restResponse.getResource();
    assertEquals(numOfApiCallLogs, savedApiCallLogs.size());
    for (int i = 0; i < numOfApiCallLogs; i++) {
      assertEquals(numOfApiCallLogs - i, savedApiCallLogs.get(i).getCreatedAt());
      assertEquals(appId, savedApiCallLogs.get(i).getAppId());
      assertEquals(accountId, savedApiCallLogs.get(i).getAccountId());
      assertEquals(stateExecutionId, savedApiCallLogs.get(i).getStateExecutionId());
      assertEquals(apiCallLogs.get(numOfApiCallLogs - i - 1).getRequestTimeStamp(),
          savedApiCallLogs.get(i).getRequestTimeStamp());
      assertEquals(apiCallLogs.get(numOfApiCallLogs - i - 1).getResponseTimeStamp(),
          savedApiCallLogs.get(i).getResponseTimeStamp());
      assertEquals(apiCallLogs.get(numOfApiCallLogs - i - 1).getRequest(), savedApiCallLogs.get(i).getRequest());
      assertEquals(apiCallLogs.get(numOfApiCallLogs - i - 1).getResponse(), savedApiCallLogs.get(i).getResponse());
    }
  }
}
