package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Status;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateService;

import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
public class DelegateServiceTest extends WingsBaseTest {
  @Inject private DelegateService delegateService;

  @Inject private WingsPersistence wingsPersistence;

  private static final Delegate.Builder BUILDER = aDelegate()
                                                      .withAccountId(ACCOUNT_ID)
                                                      .withIp("127.0.0.1")
                                                      .withHostName("localhost")
                                                      .withStatus(Status.ENABLED)
                                                      .withLastHeartBeat(System.currentTimeMillis());

  @Test
  public void shouldList() throws Exception {
    Delegate delegate = wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().build());
    assertThat(delegateService.list(aPageRequest().build())).hasSize(1).containsExactly(delegate);
  }

  @Test
  public void shouldGet() throws Exception {
    Delegate delegate = wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().build());
    assertThat(delegateService.get(ACCOUNT_ID, delegate.getUuid())).isEqualTo(delegate);
  }

  @Test
  public void shouldUpdate() throws Exception {
    Delegate delegate = wingsPersistence.saveAndGet(Delegate.class, BUILDER.but().build());
    delegate.setLastHeartBeat(System.currentTimeMillis());
    delegate.setStatus(Status.DISABLED);
    delegateService.update(delegate);
    assertThat(wingsPersistence.get(Delegate.class, delegate.getUuid())).isEqualTo(delegate);
  }

  @Test
  public void shouldAdd() throws Exception {
    Delegate delegate = delegateService.add(BUILDER.but().build());
    assertThat(wingsPersistence.get(Delegate.class, delegate.getUuid())).isEqualTo(delegate);
  }

  @Test
  public void shouldDelete() throws Exception {
    String id = wingsPersistence.save(BUILDER.but().build());
    delegateService.delete(ACCOUNT_ID, id);
    assertThat(wingsPersistence.list(Delegate.class)).hasSize(0);
  }

  @Test
  public void shouldRegister() throws Exception {
    // String id = wingsPersistence.save(BUILDER.but().build());
    Delegate delegate = delegateService.register(BUILDER.but().build());
    assertThat(delegateService.get(ACCOUNT_ID, delegate.getUuid())).isEqualTo(delegate);
  }

  @Test
  public void shouldRegisterExistingDelegate() throws Exception {
    // String id = wingsPersistence.save(BUILDER.but().build());
    Delegate delegate = delegateService.add(BUILDER.but().build());
    delegateService.register(delegate);
    assertThat(delegateService.get(ACCOUNT_ID, delegate.getUuid())).isEqualTo(delegate);
  }
}
