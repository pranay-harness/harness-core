package software.wings.graphql.datafetcher.audit;

import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.event.handler.impl.segment.SegmentHandler;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.datafetcher.Principal;
import software.wings.resources.graphql.TriggeredByType;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

import java.net.URISyntaxException;

public class ChangeContentHelperTest extends AbstractDataFetcherTest {
  @Mock private SegmentHandler segmentHandler;
  @Inject @InjectMocks private ChangeContentHelper changeContentHelper;
  @Mock private AccountService accountService;
  @Mock private UserService userService;
  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void shouldReportToSegment() throws URISyntaxException {
    Account account = anAccount()
                          .withCompanyName("company1")
                          .withAccountName("account1")
                          .withAccountKey("ACCOUNT_KEY")
                          .withUuid("accountId")
                          .withLicenseInfo(getLicenseInfo())
                          .build();
    User user = User.Builder.anUser().uuid("userId").build();
    when(accountService.get(account.getUuid())).thenReturn(account);
    Principal triggeredBy =
        Principal.builder().triggeredById("triggeredById").triggeredByType(TriggeredByType.USER).build();
    when(userService.get(triggeredBy.getTriggeredById())).thenReturn(user);
    changeContentHelper.reportAuditTrailExportToSegment("accountId", triggeredBy);
    verify(segmentHandler, times(1))
        .reportTrackEvent(eq(account), eq("Audit Trail exported"), any(User.class), anyMap(), anyMap());
  }
}
