package software.wings.service.impl;

import static io.harness.rule.OwnerRule.HANTANG;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnection;

import java.util.ArrayList;
import java.util.Arrays;

public class DelegateServiceImplCategoryTest extends CategoryTest {
  private String delegateId = "DELEGATE_ID";
  private Delegate delegate;
  private DelegateConnection delegateConnection;

  @Mock private Broadcaster broadcaster;
  @Mock private BroadcasterFactory broadcasterFactory;
  @Mock private DelegateTaskBroadcastHelper broadcastHelper;
  @Mock private DelegateConnectionDao delegateConnectionDao;
  @InjectMocks private DelegateServiceImpl delegateService;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldReturnTrueWhenDelegateConnected() {
    delegate = Delegate.builder().uuid(delegateId).connected(true).build();
    when(delegateConnectionDao.list(delegate)).thenReturn(new ArrayList<>(Arrays.asList(delegateConnection)));
    boolean result = delegateService.isDelegateConnected(delegate);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldReturnFalseWhenDelegateDisconnected() {
    delegate = Delegate.builder().uuid(delegateId).connected(true).build();
    when(delegateConnectionDao.list(delegate)).thenReturn(emptyList());
    boolean result1 = delegateService.isDelegateConnected(delegate);
    assertThat(result1).isFalse();

    delegate = Delegate.builder().uuid(delegateId).connected(false).build();
    when(delegateConnectionDao.list(delegate)).thenReturn(new ArrayList<>(Arrays.asList(delegateConnection)));
    boolean result2 = delegateService.isDelegateConnected(delegate);
    assertThat(result2).isFalse();
  }
}
