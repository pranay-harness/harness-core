/**
 *
 */

package software.wings.waitnotify;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.SearchFilter.Operator;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.sm.ExecutionStatus;

import javax.inject.Inject;

/**
 * Test for checking notify cleanup handler.
 *
 * @author Rishi
 */
public class NotifyResponseCleanupHandlerTest extends WingsBaseTest {
  @Inject private NotifyResponseCleanupHandler notifyResponseCleanupHandler;

  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void shouldCleanup() throws InterruptedException {
    String corrId = UUIDGenerator.getUuid();
    NotifyResponse<String> notifyResponse = new NotifyResponse<>(corrId, "TEST");
    notifyResponse.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(notifyResponse);

    PageRequest<NotifyResponse> reqNotifyRes = new PageRequest<>();
    reqNotifyRes.addFilter("status", ExecutionStatus.SUCCESS, Operator.EQ);
    reqNotifyRes.setLimit(PageRequest.UNLIMITED);
    reqNotifyRes.getFieldsIncluded().add("uuid");
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
