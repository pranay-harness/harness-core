/**
 *
 */

package software.wings.waitnotify;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.waitnotify.StringNotifyResponseData.Builder.aStringNotifyResponseData;

import com.google.inject.Inject;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.sm.ExecutionStatus;

/**
 * Test for checking notify cleanup handler.
 *
 * @author Rishi
 */
public class NotifyResponseCleanupHandlerTest extends WingsBaseTest {
  @Inject private NotifyResponseCleanupHandler notifyResponseCleanupHandler;

  @Inject private WingsPersistence wingsPersistence;

  /**
   * Should cleanup.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldCleanup() throws InterruptedException {
    String corrId = generateUuid();
    NotifyResponse<StringNotifyResponseData> notifyResponse =
        new NotifyResponse<>(corrId, aStringNotifyResponseData().withData("TEST").build());
    notifyResponse.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(notifyResponse);

    PageRequest<NotifyResponse> reqNotifyRes = new PageRequest<>();
    reqNotifyRes.addFilter("status", Operator.EQ, ExecutionStatus.SUCCESS);
    reqNotifyRes.setLimit(PageRequest.UNLIMITED);
    reqNotifyRes.addFieldsIncluded(ID_KEY);
    PageResponse<NotifyResponse> notifyPageResponses = wingsPersistence.query(NotifyResponse.class, reqNotifyRes);
    assertThat(notifyPageResponses)
        .as("NotifyResponsesWithSuccessStatus")
        .isNotNull()
        .as("NotifyResponsesWithSuccessStatusSize")
        .hasSize(1);
    notifyResponseCleanupHandler.run();

    notifyPageResponses = wingsPersistence.query(NotifyResponse.class, reqNotifyRes);
    assertThat(notifyPageResponses)
        .as("NotifyResponsesWithSuccessStatus")
        .isNotNull()
        .as("NotifyResponsesWithSuccessStatusSize")
        .hasSize(0);
  }
}
