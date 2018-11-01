package software.wings.scheduler;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.Delegate;
import software.wings.beans.alert.AlertType;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.utils.EmailHelperUtil;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class AlertCheckJobTest extends WingsBaseTest {
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  @Mock private WingsPersistence wingsPersistence;
  @Mock private AlertService alertService;
  @Mock private BackgroundJobScheduler jobScheduler;
  @Mock private ExecutorService executorService;
  @Mock DelegateService delegateService;
  @Mock EmailHelperUtil emailHelperUtil;
  @Mock MainConfiguration mainConfiguration;
  @Spy @InjectMocks AlertCheckJob alertCheckJob;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(mainConfiguration.getSmtpConfig()).thenReturn(mock(SmtpConfig.class));
    when(emailHelperUtil.isSmtpConfigValid(any(SmtpConfig.class))).thenReturn(true);
  }

  /**
   * All delegates are active
   * @throws Exception
   */
  @Test
  public void testExecuteInternal_noAlert() throws Exception {
    doReturn(asList(getDelegate("host1", 2))).when(alertCheckJob).getDelegatesForAccount(ACCOUNT_ID);
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
    doReturn(asList(getDelegate("host1", 12), getDelegate("host2", 10)))
        .when(alertCheckJob)
        .getDelegatesForAccount(ACCOUNT_ID);
    doReturn(null).when(alertService).openAlert(any(), any(), any(), any());
    doNothing().when(alertService).closeAlert(any(), any(), any(), any());
    doNothing().when(delegateService).sendAlertNotificationsForNoActiveDelegates(any());
    MethodUtils.invokeMethod(alertCheckJob, true, "executeInternal", ACCOUNT_ID);
    verify(alertService, times(1)).openAlert(any(), any(), any(), any());

    ArgumentCaptor<AlertType> captor = ArgumentCaptor.forClass(AlertType.class);
    verify(alertService).openAlert(any(), any(), captor.capture(), any());
    verify(delegateService, times(1)).sendAlertNotificationsForNoActiveDelegates(anyString());
    AlertType alertType = captor.getValue();
    assertEquals(AlertType.NoActiveDelegates, alertType);
  }

  /**
   * Some of the delegates are down
   * @throws Exception
   */
  @Test
  public void testExecuteInternal_delegatesDownAlert() throws Exception {
    doReturn(asList(getDelegate("host1", 2), getDelegate("host2", 10)))
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

  @Test
  public void testSMTPAlert() throws Exception {
    when(mainConfiguration.getSmtpConfig()).thenReturn(null);
    when(emailHelperUtil.isSmtpConfigValid(any(SmtpConfig.class))).thenReturn(false);

    MethodUtils.invokeMethod(alertCheckJob, true, "checkForInvalidValidSMTP", ACCOUNT_ID);

    verify(alertService, times(1)).openAlert(any(), any(), any(), any());

    ArgumentCaptor<AlertType> captor = ArgumentCaptor.forClass(AlertType.class);
    verify(alertService).openAlert(any(), any(), captor.capture(), any());
    AlertType alertType = captor.getValue();
    assertEquals(AlertType.INVALID_SMTP_CONFIGURATION, alertType);
  }
}
