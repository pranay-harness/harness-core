/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.IGOR;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.PcfConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.cloudProvider.QLDeleteCloudProviderInput;
import software.wings.service.intfc.SettingsService;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DeleteCloudProviderDataFetcherTest extends AbstractDataFetcherTestBase {
  private static final String CLOUD_PROVIDER_ID = "CP-ID";
  private static final String ACCOUNT_ID = "ACCOUNT-ID";

  @Mock private SettingsService settingsService;

  @InjectMocks private DeleteCloudProviderDataFetcher dataFetcher = new DeleteCloudProviderDataFetcher();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void delete() {
    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                 .withValue(PcfConfig.builder().accountId(ACCOUNT_ID).build())
                 .build())
        .when(settingsService)
        .getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);

    doNothing().when(settingsService).delete(null, CLOUD_PROVIDER_ID);

    dataFetcher.mutateAndFetch(QLDeleteCloudProviderInput.builder().cloudProviderId(CLOUD_PROVIDER_ID).build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).delete(null, CLOUD_PROVIDER_ID);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void deleteWithoutIdParameter() {
    dataFetcher.mutateAndFetch(
        QLDeleteCloudProviderInput.builder().build(), MutationContext.builder().accountId(ACCOUNT_ID).build());
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void deleteOfWrongCategory() {
    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withCategory(SettingAttribute.SettingCategory.AZURE_ARTIFACTS)
                 .withValue(PcfConfig.builder().accountId(ACCOUNT_ID).build())
                 .build())
        .when(settingsService)
        .getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);

    dataFetcher.mutateAndFetch(QLDeleteCloudProviderInput.builder().cloudProviderId(CLOUD_PROVIDER_ID).build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(0)).delete(null, CLOUD_PROVIDER_ID);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void deleteOfNonExistingSetting() {
    doReturn(null).when(settingsService).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);

    dataFetcher.mutateAndFetch(QLDeleteCloudProviderInput.builder().cloudProviderId(CLOUD_PROVIDER_ID).build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(0)).delete(null, CLOUD_PROVIDER_ID);
  }
}
