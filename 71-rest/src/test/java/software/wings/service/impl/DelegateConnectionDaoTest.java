package software.wings.service.impl;

import static io.harness.rule.OwnerRule.HANTANG;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnection;

import java.util.List;

public class DelegateConnectionDaoTest extends WingsBaseTest {
  private String delegateId = "DELEGATE_ID";
  private Delegate delegate;
  private DelegateConnection delegateConnection;

  @Inject private DelegateConnectionDao delegateConnectionDao;

  @Before
  public void setUp() {
    delegate = Delegate.builder().uuid(delegateId).build();
    delegateConnection = DelegateConnection.builder().delegateId(delegateId).build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldList() {
    delegateConnectionDao.save(delegateConnection);
    List<DelegateConnection> delegateConnections = delegateConnectionDao.list(delegate);
    assertThat(delegateConnections.get(0)).isEqualTo(delegateConnection);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldReturnNullWhenList() {
    List<DelegateConnection> delegateConnections = delegateConnectionDao.list(delegate);
    assertThat(delegateConnections).isEqualTo(emptyList());
  }
}
