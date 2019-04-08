package software.wings.scheduler;

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
import static software.wings.beans.ManagerConfiguration.Builder.aManagerConfiguration;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnection;
import software.wings.beans.alert.AlertType;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.utils.EmailHelperUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AlertCheckJobTest extends WingsBaseTest {
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  @Mock private AlertService alertService;
  @Mock private DelegateService delegateService;
  @Mock private EmailHelperUtil emailHelperUtil;
  @Mock private MainConfiguration mainConfiguration;
  @InjectMocks @Inject AlertCheckJob alertCheckJob;
  @Inject private WingsPersistence wingsPersistence;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(mainConfiguration.getSmtpConfig()).thenReturn(mock(SmtpConfig.class));
    when(emailHelperUtil.isSmtpConfigValid(any(SmtpConfig.class))).thenReturn(true);
    wingsPersistence.save(aManagerConfiguration().withPrimaryVersion("*").build());
  }

  /**
   * All delegates are active
   */
  @Test
  @Category(UnitTests.class)
  public void testExecuteInternal_noAlert() {
    saveDelegate("host1", 2, true);
    doNothing().when(alertService).closeAlert(any(), any(), any(), any());
    alertCheckJob.executeInternal(ACCOUNT_ID);
    verify(alertService, times(1)).closeAlert(any(), any(), any(), any());
  }

  /**
   * All delegates are down
   */
  @Test
  @Category(UnitTests.class)
  public void testExecuteInternal_noDelegateAlert() {
    saveDelegate("host1", 12, false);
    saveDelegate("host2", 10, false);
    doReturn(null).when(alertService).openAlert(any(), any(), any(), any());
    doNothing().when(alertService).closeAlert(any(), any(), any(), any());
    doNothing().when(delegateService).sendAlertNotificationsForNoActiveDelegates(any());
    alertCheckJob.executeInternal(ACCOUNT_ID);
    verify(alertService, times(1)).openAlert(any(), any(), any(), any());

    ArgumentCaptor<AlertType> captor = ArgumentCaptor.forClass(AlertType.class);
    verify(alertService).openAlert(any(), any(), captor.capture(), any());
    verify(delegateService, times(1)).sendAlertNotificationsForNoActiveDelegates(anyString());
    AlertType alertType = captor.getValue();
    assertEquals(AlertType.NoActiveDelegates, alertType);
  }

  /**
   * Some of the delegates are down
   */
  @Test
  @Category(UnitTests.class)
  public void testExecuteInternal_delegatesDownAlert() {
    saveDelegate("host1", 2, true);
    saveDelegate("host2", 10, false);

    doNothing().when(delegateService).sendAlertNotificationsForDownDelegates(any(), any());
    doNothing().when(alertService).closeAlert(any(), any(), any(), any());

    alertCheckJob.executeInternal(ACCOUNT_ID);
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

  private void saveDelegate(String host, int timeAfterLastHB, boolean createConnection) {
    Delegate delegate = new Delegate();
    delegate.setHostName(host);
    long lastHeartbeat = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(timeAfterLastHB);
    delegate.setLastHeartBeat(lastHeartbeat);
    delegate.setAccountId(ACCOUNT_ID);
    wingsPersistence.save(delegate);

    if (createConnection) {
      DelegateConnection connection = DelegateConnection.builder()
                                          .accountId(ACCOUNT_ID)
                                          .delegateId(delegate.getUuid())
                                          .lastHeartbeat(lastHeartbeat)
                                          .build();
      wingsPersistence.save(connection);
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testSMTPAlert() {
    when(mainConfiguration.getSmtpConfig()).thenReturn(null);
    when(emailHelperUtil.isSmtpConfigValid(any(SmtpConfig.class))).thenReturn(false);

    alertCheckJob.checkForInvalidValidSMTP(ACCOUNT_ID);
    verify(alertService, times(1)).openAlert(any(), any(), any(), any());

    ArgumentCaptor<AlertType> captor = ArgumentCaptor.forClass(AlertType.class);
    verify(alertService).openAlert(any(), any(), captor.capture(), any());
    AlertType alertType = captor.getValue();
    assertEquals(AlertType.INVALID_SMTP_CONFIGURATION, alertType);
  }
}
