package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.HANTANG;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateConnectionHeartbeat;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateConnection.DelegateConnectionKeys;
import software.wings.beans.DelegateStatus;

import java.util.List;
import java.util.Map;

public class DelegateConnectionDaoTest extends WingsBaseTest {
  private String delegateId = "DELEGATE_ID";
  private String delegateId2 = "DELEGATE_ID2";
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
    List<DelegateConnection> delegateConnections =
        delegateConnectionDao.list(delegate.getAccountId(), delegate.getUuid());
    assertThat(delegateConnections.get(0)).isEqualTo(delegateConnection);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldReturnNullWhenList() {
    List<DelegateConnection> delegateConnections =
        delegateConnectionDao.list(delegate.getAccountId(), delegate.getUuid());
    assertThat(delegateConnections).isEqualTo(emptyList());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldDoConnectionHeartbeat() {
    delegateConnectionDao.registerHeartbeat(ACCOUNT_ID, DELEGATE_ID,
        DelegateConnectionHeartbeat.builder().delegateConnectionId(generateUuid()).version("1.0.1").build());
    DelegateConnection connection = wingsPersistence.createQuery(DelegateConnection.class)
                                        .filter(DelegateConnectionKeys.accountId, ACCOUNT_ID)
                                        .get();
    assertThat(connection.getVersion()).isEqualTo("1.0.1");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldObtainActiveDelegateConnections() {
    delegateConnectionDao.registerHeartbeat(ACCOUNT_ID, delegateId,
        DelegateConnectionHeartbeat.builder().delegateConnectionId(generateUuid()).version("1.0.1").build());
    delegateConnectionDao.registerHeartbeat(ACCOUNT_ID, delegateId,
        DelegateConnectionHeartbeat.builder().delegateConnectionId(generateUuid()).version("1.0.2").build());
    delegateConnectionDao.registerHeartbeat(ACCOUNT_ID, delegateId2,
        DelegateConnectionHeartbeat.builder().delegateConnectionId(generateUuid()).version("1.0.1").build());

    Map<String, List<DelegateStatus.DelegateInner.DelegateConnectionInner>> delegateConnections =
        delegateConnectionDao.obtainActiveDelegateConnections(ACCOUNT_ID);

    assertThat(delegateConnections).hasSize(2);
    assertThat(delegateConnections.get(delegateId)).hasSize(2);
    assertThat(delegateConnections.get(delegateId2)).hasSize(1);
  }
}
