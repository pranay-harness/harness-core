package software.wings.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static software.wings.beans.History.Builder.aHistory;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.History;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HistoryService;

/**
 * Created by peeyushaggarwal on 6/20/16.
 */
public class HistoryServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;

  @InjectMocks @Inject private HistoryService historyService;

  /**
   * Should create history.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldCreateHistory() throws Exception {
    historyService.create(aHistory().withAppId(APP_ID).build());
    verify(wingsPersistence).save(any(History.class));
  }

  /**
   * Should list history.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldListHistory() throws Exception {
    historyService.list(new PageRequest<>());
    verify(wingsPersistence).query(eq(History.class), any(PageRequest.class));
  }
}
