package software.wings.graphql.datafetcher.connector;

import static io.harness.rule.OwnerRule.TMACARI;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.GitConfig;
import software.wings.beans.PcfConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.connector.input.QLDeleteConnectorInput;
import software.wings.service.intfc.SettingsService;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DeleteConnectorDataFetcherTest extends AbstractDataFetcherTestBase {
  private static final String CONNECTOR_ID = "CP-ID";
  private static final String ACCOUNT_ID = "ACCOUNT-ID";

  @Mock private SettingsService settingsService;

  @InjectMocks private DeleteConnectorDataFetcher dataFetcher = new DeleteConnectorDataFetcher();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void delete() {
    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withCategory(SettingAttribute.SettingCategory.CONNECTOR)
                 .withValue(GitConfig.builder().accountId(ACCOUNT_ID).build())
                 .build())
        .when(settingsService)
        .getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    doNothing().when(settingsService).delete(null, CONNECTOR_ID);

    dataFetcher.mutateAndFetch(QLDeleteConnectorInput.builder().connectorId(CONNECTOR_ID).build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).delete(null, CONNECTOR_ID);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void deleteWithoutIdParameter() {
    dataFetcher.mutateAndFetch(
        QLDeleteConnectorInput.builder().build(), MutationContext.builder().accountId(ACCOUNT_ID).build());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void deleteOfWrongCategory() {
    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withCategory(SettingAttribute.SettingCategory.AZURE_ARTIFACTS)
                 .withValue(PcfConfig.builder().accountId(ACCOUNT_ID).build())
                 .build())
        .when(settingsService)
        .getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    dataFetcher.mutateAndFetch(QLDeleteConnectorInput.builder().connectorId(CONNECTOR_ID).build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(0)).delete(null, CONNECTOR_ID);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void deleteOfNonExistingSetting() {
    doReturn(null).when(settingsService).getByAccount(ACCOUNT_ID, CONNECTOR_ID);

    dataFetcher.mutateAndFetch(QLDeleteConnectorInput.builder().connectorId(CONNECTOR_ID).build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(0)).delete(null, CONNECTOR_ID);
  }
}
