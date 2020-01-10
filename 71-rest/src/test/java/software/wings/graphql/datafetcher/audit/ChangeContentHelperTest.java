package software.wings.graphql.datafetcher.audit;

import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.event.handler.impl.segment.SegmentHandler;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.datafetcher.AccountThreadLocal;
import software.wings.security.UserThreadLocal;

import java.net.URISyntaxException;
import java.util.Arrays;

public class ChangeContentHelperTest extends AbstractDataFetcherTest {
  @Mock private SegmentHandler segmentHandler;
  @Inject @InjectMocks private ChangeContentHelper changeContentHelper;

  Account account;
  User user;
  @Before
  public void setUp() {
    account = getAccount(AccountType.PAID);
    user = new User();
    user.setAccounts(Arrays.asList(account));
    UserThreadLocal.set(user);
    AccountThreadLocal.set(account.getUuid());
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void shouldReportToSegment() throws URISyntaxException {
    changeContentHelper.reportAuditTrailExportToSegment();
    verify(segmentHandler, times(1))
        .reportTrackEvent(eq(account), eq("Audit Trail exported"), eq(user), anyMap(), anyMap());
  }
}
