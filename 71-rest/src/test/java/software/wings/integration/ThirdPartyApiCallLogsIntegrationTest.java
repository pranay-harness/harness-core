package software.wings.integration;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;

import com.google.common.collect.Lists;

import io.harness.beans.PageResponse;
import io.harness.category.element.IntegrationTests;
import io.harness.data.structure.CollectionUtils;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 6/14/18.
 */
public class ThirdPartyApiCallLogsIntegrationTest extends BaseIntegrationTest {
  private String stateExecutionId;
  private String appId;
  private String delegateId;
  private String delegateTaskId;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    stateExecutionId = generateUuid();
    appId = wingsPersistence.save(anApplication().name(generateUuid()).accountId(accountId).build());
    delegateId = generateUuid();
    delegateTaskId = generateUuid();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(IntegrationTests.class)
  public void testSaveApiCallLogs() throws Exception {
    int numOfApiCallLogs = 12;
    List<ThirdPartyApiCallLog> apiCallLogs = new ArrayList<>();
    for (int i = 0; i < numOfApiCallLogs; i++) {
      apiCallLogs.add(ThirdPartyApiCallLog.builder()
                          .accountId(accountId)
                          .stateExecutionId(stateExecutionId)
                          .requestTimeStamp(i)
                          .request(Lists.newArrayList(ThirdPartyApiCallField.builder()
                                                          .name(generateUuid())
                                                          .value(generateUuid())
                                                          .type(FieldType.TEXT)
                                                          .build()))
                          .response(Lists.newArrayList(ThirdPartyApiCallField.builder()
                                                           .name(generateUuid())
                                                           .value(generateUuid())
                                                           .type(FieldType.TEXT)
                                                           .build()))
                          .delegateId(delegateId)
                          .delegateTaskId(delegateTaskId)
                          .build());
    }
    WebTarget target = client.target(API_BASE + "/delegates/" + delegateTaskId + "/state-executions"
        + "?accountId=" + accountId);
    RestResponse<Boolean> restResponse = getDelegateRequestBuilderWithAuthHeader(target).post(
        entity(apiCallLogs, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    Thread.sleep(2000);

    target = client.target(
        API_BASE + "/activities/" + stateExecutionId + "/api-call-logs?accountId=" + accountId + "&appId=" + appId);
    RestResponse<PageResponse<ThirdPartyApiCallLog>> logResponse = getRequestBuilderWithAuthHeader(target).get(
        new GenericType<RestResponse<PageResponse<ThirdPartyApiCallLog>>>() {});

    List<ThirdPartyApiCallLog> savedApiCallLogs = logResponse.getResource().getResponse();
    assertThat(savedApiCallLogs).hasSize(numOfApiCallLogs);
    savedApiCallLogs.forEach(savedApiCallLog -> {
      assertThat(savedApiCallLog.getUuid()).isNotNull();
      savedApiCallLog.setUuid(null);
      assertThat(savedApiCallLog.getCreatedAt() > 0).isTrue();
      savedApiCallLog.setCreatedAt(0);
    });

    assertThat(CollectionUtils.isEqualCollection(apiCallLogs, savedApiCallLogs)).isTrue();
  }
}
