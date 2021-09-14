/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.scheduler;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.VUK;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.ManagerConfiguration.Builder.aManagerConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.alert.DelegatesScalingGroupDownAlert;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.DelegateConnection;
import software.wings.beans.alert.AlertType;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.utils.EmailHelperUtils;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AlertCheckJobTest extends WingsBaseTest {
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  @Mock private AlertService alertService;
  @Mock private DelegateService delegateService;
  @Mock private EmailHelperUtils emailHelperUtils;
  @Mock private MainConfiguration mainConfiguration;
  @InjectMocks @Inject AlertCheckJob alertCheckJob;
  @Inject private HPersistence persistence;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(mainConfiguration.getSmtpConfig()).thenReturn(mock(SmtpConfig.class));
    when(emailHelperUtils.isSmtpConfigValid(any(SmtpConfig.class))).thenReturn(true);
    persistence.save(aManagerConfiguration().withPrimaryVersion("*").build());
  }

  /**
   * All delegates are active
   */
  @Test
  @Owner(developers = ADWAIT)
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
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testExecuteInternal_noDelegateAlert() {
    saveDelegate("host1", 12, false);
    saveDelegate("host2", 10, false);
    doReturn(null).when(alertService).openAlert(any(), any(), any(), any());
    doNothing().when(alertService).closeAlert(any(), any(), any(), any());
    alertCheckJob.executeInternal(ACCOUNT_ID);
    verify(alertService, times(1)).openAlert(any(), any(), any(), any());

    ArgumentCaptor<AlertType> captor = ArgumentCaptor.forClass(AlertType.class);
    verify(alertService).openAlert(any(), any(), captor.capture(), any());
    AlertType alertType = captor.getValue();
    assertThat(alertType).isEqualTo(AlertType.NoActiveDelegates);
  }

  /**
   * Some of the delegates are down
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testExecuteInternal_delegatesDownAlert() {
    saveDelegate("host1", 2, true);
    saveDelegate("host2", 10, false);

    doNothing().when(alertService).closeAlert(any(), any(), any(), any());

    alertCheckJob.executeInternal(ACCOUNT_ID);
    verify(alertService, times(1)).closeAlert(any(), any(), any(), any());
  }

  private void saveDelegate(String host, int timeAfterLastHB, boolean createConnection) {
    long lastHeartbeat = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(timeAfterLastHB);
    Delegate delegate = Delegate.builder().accountId(ACCOUNT_ID).hostName(host).lastHeartBeat(lastHeartbeat).build();
    persistence.save(delegate);

    if (createConnection) {
      DelegateConnection connection = DelegateConnection.builder()
                                          .accountId(ACCOUNT_ID)
                                          .delegateId(delegate.getUuid())
                                          .lastHeartbeat(lastHeartbeat)
                                          .build();
      persistence.save(connection);
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testSMTPAlert() {
    when(mainConfiguration.getSmtpConfig()).thenReturn(null);
    when(emailHelperUtils.isSmtpConfigValid(any(SmtpConfig.class))).thenReturn(false);

    alertCheckJob.checkForInvalidValidSMTP(ACCOUNT_ID);
    verify(alertService, times(1)).openAlert(any(), any(), any(), any());

    ArgumentCaptor<AlertType> captor = ArgumentCaptor.forClass(AlertType.class);
    verify(alertService).openAlert(any(), any(), captor.capture(), any());
    AlertType alertType = captor.getValue();
    assertThat(alertType).isEqualTo(AlertType.INVALID_SMTP_CONFIGURATION);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldOpenAlertWhenEveryDelegateInGroupIsDown() {
    String delegateGroupName = generateUuid();

    long lastHeartbeatDisconnected = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);

    Delegate delegate1 = Delegate.builder()
                             .accountId(ACCOUNT_ID)
                             .hostName("host1")
                             .delegateGroupName(delegateGroupName)
                             .lastHeartBeat(lastHeartbeatDisconnected)
                             .build();
    persistence.save(delegate1);

    Delegate delegate2 = Delegate.builder()
                             .accountId(ACCOUNT_ID)
                             .hostName("host1")
                             .delegateGroupName(delegateGroupName)
                             .lastHeartBeat(lastHeartbeatDisconnected)
                             .build();
    persistence.save(delegate2);

    List<Delegate> delegates = Arrays.asList(delegate1, delegate2);

    Set<String> primaryConnections = new HashSet();
    primaryConnections.add(delegateGroupName);

    alertCheckJob.processDelegateWhichBelongsToGroup(ACCOUNT_ID, delegates, primaryConnections);

    verify(alertService, times(1))
        .openAlert(eq(ACCOUNT_ID), eq(GLOBAL_APP_ID), eq(AlertType.DelegatesScalingGroupDownAlert),
            eq(DelegatesScalingGroupDownAlert.builder().accountId(ACCOUNT_ID).groupName(delegateGroupName).build()));
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldCloseAlertWhenDelegateWithGroupIsActive() {
    String delegateGroupName = generateUuid();

    long lastHeartbeatConnected = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(2);
    Delegate delegate = Delegate.builder()
                            .uuid(generateUuid())
                            .accountId(ACCOUNT_ID)
                            .hostName("host1")
                            .delegateGroupName(delegateGroupName)
                            .lastHeartBeat(lastHeartbeatConnected)
                            .build();
    persistence.save(delegate);

    List<Delegate> delegates = Arrays.asList(delegate);
    Set<String> primaryConnections = new HashSet();
    primaryConnections.add(delegate.getUuid());

    alertCheckJob.processDelegateWhichBelongsToGroup(ACCOUNT_ID, delegates, primaryConnections);

    verify(alertService, times(1))
        .closeAlert(eq(ACCOUNT_ID), eq(GLOBAL_APP_ID), eq(AlertType.DelegatesScalingGroupDownAlert),
            eq(DelegatesScalingGroupDownAlert.builder().accountId(ACCOUNT_ID).groupName(delegateGroupName).build()));
  }
}
