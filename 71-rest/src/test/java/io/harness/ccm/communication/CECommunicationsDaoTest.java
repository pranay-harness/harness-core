package io.harness.ccm.communication;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.communication.entities.CECommunications;
import io.harness.ccm.communication.entities.CommunicationType;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.List;

public class CECommunicationsDaoTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private CECommunications communications;
  private String email = "user@harness.io";
  private CommunicationType type = CommunicationType.WEEKLY_REPORT;
  private boolean enable = true;

  @Inject private CECommunicationsDao communicationsDao;

  @Before
  public void setUp() {
    communications = CECommunications.builder()
                         .accountId(accountId)
                         .emailId(email)
                         .type(CommunicationType.WEEKLY_REPORT)
                         .enabled(enable)
                         .build();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testSave() {
    String uuid = communicationsDao.save(communications);
    assertThat(uuid).isNotNull();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testSaveWithAccountId() {
    String uuid = communicationsDao.save(accountId, email, type, enable);
    assertThat(uuid).isNotNull();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGet() {
    String uuid = communicationsDao.save(communications);
    CECommunications result = communicationsDao.get(accountId, email, type);
    assertThat(result).isNotNull();
    assertThat(result.getUuid()).isEqualTo(uuid);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testList() {
    String uuid = communicationsDao.save(communications);
    List<CECommunications> result = communicationsDao.list(accountId, email);
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0).getUuid()).isEqualTo(uuid);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testUpdate() {
    String uuid = communicationsDao.save(communications);
    communicationsDao.update(accountId, email, type, false);
    CECommunications result = communicationsDao.get(accountId, email, type);
    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isEqualTo(false);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testDelete() {
    String uuid = communicationsDao.save(communications);
    boolean successful = communicationsDao.delete(uuid);
    assertThat(successful).isTrue();
  }
}
