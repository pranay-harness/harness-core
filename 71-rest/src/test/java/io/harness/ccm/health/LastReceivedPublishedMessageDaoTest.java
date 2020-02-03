package io.harness.ccm.health;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.LastReceivedPublishedMessage;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class LastReceivedPublishedMessageDaoTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private String identifier = "IDENTIFIER";
  @Inject private LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldGetPublishedMessage() {
    lastReceivedPublishedMessageDao.upsert(accountId, identifier);
    LastReceivedPublishedMessage message = lastReceivedPublishedMessageDao.get(accountId, identifier);
    assertThat(message.getAccountId()).isEqualTo(accountId);
    assertThat(message.getIdentifier()).isEqualTo(identifier);
  }
}
