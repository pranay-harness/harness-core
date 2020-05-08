package software.wings.resources;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.VUK;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.utils.ResourceTestRule;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.GenericType;

public class DelegateSelectionLogResourceTest {
  private static HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
  private static DelegateSelectionLogsService delegateSelectionLogsService = mock(DelegateSelectionLogsService.class);

  @ClassRule
  public static final ResourceTestRule RESOURCES =

      ResourceTestRule.builder()
          .addResource(new DelegateSelectionLogResource(delegateSelectionLogsService))
          .addResource(new AbstractBinder() {
            @Override
            protected void configure() {
              bind(httpServletRequest).to(HttpServletRequest.class);
            }
          })
          .addProvider(WingsExceptionMapper.class)
          .build();

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void getSelectionLogs() {
    String accountId = generateUuid();
    String taskId = generateUuid();
    RestResponse<List<DelegateSelectionLogParams>> restResponse =
        RESOURCES.client()
            .target("/selection-logs?accountId=" + accountId + "&taskId=" + taskId)
            .request()
            .get(new GenericType<RestResponse<List<DelegateSelectionLogParams>>>() {});

    verify(delegateSelectionLogsService, atLeastOnce()).fetchTaskSelectionLogs(accountId, taskId);
  }
}
