package io.harness.ccm.communication;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.communication.entities.CECommunications;
import io.harness.ccm.communication.entities.CommunicationType;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

public class CECommunicationsServiceImplTest extends WingsBaseTest {
  @Mock private CECommunicationsDao communicationsDao;
  @Inject @InjectMocks private CECommunicationsServiceImpl communicationsService;

  private String accountId = "ACCOUNT_ID";
  private String accountId2 = "ACCOUNT_ID2";
  private String uuid = "uuid";
  private CECommunications communications;
  private CECommunications newCommunications;
  private String email = "user@harness.io";
  private String defaultEmail = "default@harness.io";
  private CommunicationType type = CommunicationType.WEEKLY_REPORT;
  private boolean enable = true;

  @Before
  public void setUp() {
    communications =
        CECommunications.builder().accountId(accountId).uuid(uuid).emailId(email).type(type).enabled(enable).build();
    newCommunications =
        CECommunications.builder().accountId(accountId2).emailId(defaultEmail).type(type).enabled(enable).build();
    when(communicationsDao.get(accountId, email, type)).thenReturn(communications);
    when(communicationsDao.get(accountId2, defaultEmail, type)).thenReturn(null);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGet() throws Exception {
    CECommunications result = communicationsService.get(accountId, email, type);
    assertThat(result).isNotNull();
    assertThat(result.getUuid()).isEqualTo(uuid);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testUpdate() throws Exception {
    communicationsService.update(accountId, email, type, false);
    verify(communicationsDao).update(eq(accountId), eq(email), eq(type), eq(false));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testUpdateAsSave() throws Exception {
    communicationsService.update(accountId2, defaultEmail, type, true);
    verify(communicationsDao).save(eq(newCommunications));
  }
}
