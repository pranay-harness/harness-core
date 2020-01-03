package software.wings.graphql.datafetcher.service;

import static io.harness.rule.OwnerRule.RUSHABH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Service;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.query.QLServiceQueryParameters;
import software.wings.graphql.schema.type.QLService;
import software.wings.security.UserThreadLocal;

import java.sql.SQLException;

public class ServiceDataFetcherTest extends AbstractDataFetcherTest {
  @Inject ServiceDataFetcher serviceDataFetcher;
  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testServiceDataFetcher() {
    Service service = createService(ACCOUNT1_ID, APP1_ID_ACCOUNT1, SERVICE1_ID_APP1_ACCOUNT1, SERVICE1_ID_APP1_ACCOUNT1,
        TAG_MODULE, TAG_VALUE_MODULE1);
    QLService qlService = serviceDataFetcher.fetch(
        QLServiceQueryParameters.builder().serviceId(SERVICE1_ID_APP1_ACCOUNT1).build(), ACCOUNT1_ID);
    assertThat(qlService.getId()).isEqualTo(SERVICE1_ID_APP1_ACCOUNT1);
    assertThat(qlService.getName()).isEqualTo(SERVICE1_ID_APP1_ACCOUNT1);

    qlService = serviceDataFetcher.fetch(
        QLServiceQueryParameters.builder().serviceId(SERVICE2_ID_APP1_ACCOUNT1).build(), ACCOUNT1_ID);
    assertThat(qlService).isNull();

    try {
      qlService = serviceDataFetcher.fetch(
          QLServiceQueryParameters.builder().serviceId(SERVICE1_ID_APP1_ACCOUNT1).build(), ACCOUNT2_ID);

      fail("InvalidRequestException expected here");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(InvalidRequestException.class);
    }
  }
}
