package io.harness.marketplace.gcp.events;

import static io.harness.rule.OwnerRule.JATIN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.marketplace.gcp.events.AccountActiveEvent.Account;
import io.harness.marketplace.gcp.events.GcpMarketplaceEvent.GcpMarketplaceEventKeys;
import io.harness.rule.OwnerRule.Owner;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.IntegrationTestUtils;

import java.net.URISyntaxException;
import java.util.Optional;

public class GcpMarketplaceEventServiceImplTest extends BaseIntegrationTest {
  @Inject private GcpMarketplaceEventService service;
  @Inject private WingsPersistence persistence;

  // namespacing so that other tests are not impacted by this
  private static final String NAMESPACE = GcpMarketplaceEventServiceImplTest.class.getSimpleName();

  private boolean indexesEnsured;

  @Before
  public void ensureIndices() throws URISyntaxException {
    if (!indexesEnsured && !IntegrationTestUtils.isManagerRunning(client)) {
      persistence.getDatastore(GcpMarketplaceEvent.class).ensureIndexes(GcpMarketplaceEvent.class);
      indexesEnsured = true;
    }
  }

  @After
  public void clearCollection() {
    val ds = persistence.getDatastore(GcpMarketplaceEvent.class);
    ds.delete(ds.createQuery(GcpMarketplaceEvent.class).field(GcpMarketplaceEventKeys.messageId).endsWith(NAMESPACE));
  }

  @Test
  @Owner(developers = JATIN)
  @Category(IntegrationTests.class)
  public void testSave() {
    String messageId = "message-id-" + NAMESPACE;
    AccountActiveEvent event = new AccountActiveEvent(
        "some-event-id", EventType.ACCOUNT_ACTIVE, new Account("some-acc-id", "2019-06-12T18:55:24.707Z"));
    service.save(new GcpMarketplaceEvent(messageId, event));

    Optional<GcpMarketplaceEvent> savedEvent = service.getEvent("some-acc-id", EventType.ACCOUNT_ACTIVE);
    assertThat(savedEvent.isPresent()).isTrue();
    assertThat(event).isEqualTo(savedEvent.get().getEvent());
  }
}