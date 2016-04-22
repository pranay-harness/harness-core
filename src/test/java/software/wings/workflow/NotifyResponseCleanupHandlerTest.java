/**
 *
 */

package software.wings.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.Test;

import software.wings.WingsBaseTest;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.SearchFilter.OP;
import software.wings.common.UUIDGenerator;
import software.wings.dl.WingsPersistence;
import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.NotifyResponse;
import software.wings.waitnotify.NotifyResponseCleanupHandler;

/**
 * @author Rishi
 */
public class NotifyResponseCleanupHandlerTest extends WingsBaseTest {
  @Inject private NotifyResponseCleanupHandler notifyResponseCleanupHandler;

  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void shouldCleanup() throws InterruptedException {
    String corrId = UUIDGenerator.getUuid();
    NotifyResponse notifyResponse = new NotifyResponse(corrId, "TEST");
    notifyResponse.setStatus(ExecutionStatus.SUCCESS);
    wingsPersistence.save(notifyResponse);

    PageRequest<NotifyResponse> reqNotifyRes = new PageRequest<>();
    reqNotifyRes.addFilter("status", ExecutionStatus.SUCCESS, OP.EQ);
    reqNotifyRes.setLimit(PageRequest.UNLIMITED);
    reqNotifyRes.getFieldsIncluded().add("uuid");
    PageResponse<NotifyResponse> notifyPageResponses = wingsPersistence.query(NotifyResponse.class, reqNotifyRes);
    assertThat(notifyPageResponses).as("NotifyResponsesWithSuccessStatus").isNotNull();
    assertThat(notifyPageResponses.getResponse()).as("NotifyResponsesWithSuccessStatus").isNotNull();
    assertThat(notifyPageResponses.getResponse().size()).as("NotifyResponsesWithSuccessStatusSize").isEqualTo(1);
    notifyResponseCleanupHandler.run();

    notifyPageResponses = wingsPersistence.query(NotifyResponse.class, reqNotifyRes);
    assertThat(notifyPageResponses).as("NotifyResponsesWithSuccessStatus").isNotNull();
    assertThat(notifyPageResponses.getResponse()).as("NotifyResponsesWithSuccessStatus").isNotNull();
    assertThat(notifyPageResponses.getResponse().size()).as("NotifyResponsesWithSuccessStatusSize").isEqualTo(0);
  }
}
