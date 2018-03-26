package software.wings.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.beans.Delegate;
import software.wings.beans.alert.AlertType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class AlertCheckJobTest {
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  @Mock private WingsPersistence wingsPersistence;
  @Mock private AlertService alertService;
  @Mock private JobScheduler jobScheduler;
  @Mock private ExecutorService executorService;
  @Mock DelegateService delegateService;
  @Spy @InjectMocks AlertCheckJob alertCheckJob;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  /**
   * All delegates are active
   * @throws Exception
   */
  @Test
  public void testExecuteInternal_noAlert() throws Exception {
    doReturn(Arrays.asList(getDelegate("host1", 2))).when(alertCheckJob).getDelegatesForAccount(ACCOUNT_ID);
    doNothing().when(alertService).closeAlert(any(), any(), any(), any());

    MethodUtils.invokeMethod(alertCheckJob, true, "executeInternal", ACCOUNT_ID);
    verify(alertService, times(1)).closeAlert(any(), any(), any(), any());
  }

  /**
   * All delegates are down
   * @throws Exception
   */
  @Test
  public void testExecuteInternal_noDelegateAlert() throws Exception {
    doReturn(Arrays.asList(getDelegate("host1", 12), getDelegate("host2", 10)))
        .when(alertCheckJob)
        .getDelegatesForAccount(ACCOUNT_ID);
    doReturn(null).when(alertService).openAlert(any(), any(), any(), any());
    doNothing().when(alertService).closeAlert(any(), any(), any(), any());

    MethodUtils.invokeMethod(alertCheckJob, true, "executeInternal", ACCOUNT_ID);
    verify(alertService, times(1)).openAlert(any(), any(), any(), any());

    ArgumentCaptor<AlertType> captor = ArgumentCaptor.forClass(AlertType.class);
    verify(alertService).openAlert(any(), any(), captor.capture(), any());
    AlertType alertType = captor.getValue();
    assertEquals(AlertType.NoActiveDelegates, alertType);
  }

  /**
   * Some of the delegates are down
   * @throws Exception
   */
  @Test
  public void testExecuteInternal_delegatesDownAlert() throws Exception {
    doReturn(Arrays.asList(getDelegate("host1", 2), getDelegate("host2", 10)))
        .when(alertCheckJob)
        .getDelegatesForAccount(ACCOUNT_ID);

    doNothing().when(delegateService).sendAlertNotificationsForDownDelegates(any(), any());
    doNothing().when(alertService).closeAlert(any(), any(), any(), any());

    MethodUtils.invokeMethod(alertCheckJob, true, "executeInternal", ACCOUNT_ID);
    verify(alertService, times(1)).closeAlert(any(), any(), any(), any());
    verify(delegateService, times(1)).sendAlertNotificationsForDownDelegates(any(), any());

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(delegateService).sendAlertNotificationsForDownDelegates(any(), captor.capture());
    List list = captor.getValue();
    assertNotNull(list);
    assertEquals(1, list.size());
    Delegate delegate = (Delegate) list.get(0);
    assertEquals("host2", delegate.getHostName());
  }

  private Delegate getDelegate(String host, int timeAfterLastHB) {
    Delegate delegate = new Delegate();
    delegate.setHostName(host);
    delegate.setLastHeartBeat(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(timeAfterLastHB));
    delegate.setAccountId(ACCOUNT_ID);
    return delegate;
  }
}
