package io.harness.ccm.health;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.HANTANG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.LastReceivedPublishedMessage;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CE)
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
    assertThat(message.getAccountId()).isEqualTo("smkfsdkmsfd");
    assertThat(message.getIdentifier()).isEqualTo(identifier);
  }
}
