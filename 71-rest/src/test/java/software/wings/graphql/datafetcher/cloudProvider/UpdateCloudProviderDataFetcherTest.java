package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.IGOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.PcfConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLPcfCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateCloudProviderPayload;
import software.wings.graphql.schema.type.QLCloudProviderType;
import software.wings.graphql.schema.type.cloudProvider.QLPcfCloudProvider;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.service.intfc.SettingsService;

import java.sql.SQLException;

public class UpdateCloudProviderDataFetcherTest extends AbstractDataFetcherTest {
  private static final String CLOUD_PROVIDER_ID = "CP-ID";
  private static final String ACCOUNT_ID = "ACCOUNT-ID";

  @Mock private SettingsService settingsService;
  @Mock private UsageScopeController usageScopeController;
  @Mock private SettingServiceHelper settingServiceHelper;

  @InjectMocks private UpdateCloudProviderDataFetcher dataFetcher = new UpdateCloudProviderDataFetcher();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void update() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                                   .withValue(PcfConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                 .withValue(PcfConfig.builder().accountId(ACCOUNT_ID).build())
                 .build())
        .when(settingsService)
        .updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);

    doNothing()
        .when(settingServiceHelper)
        .updateEncryptedFieldsInResponse(isA(SettingAttribute.class), isA(Boolean.class));

    QLUpdateCloudProviderPayload payload =
        dataFetcher.mutateAndFetch(QLUpdateCloudProviderInput.builder()
                                       .cloudProviderId(CLOUD_PROVIDER_ID)
                                       .cloudProviderType(QLCloudProviderType.PCF)
                                       .pcfCloudProvider(QLPcfCloudProviderInput.builder()
                                                             .name(RequestField.ofNullable("NAME"))
                                                             .endpointUrl(RequestField.ofNullable("URL"))
                                                             .userName(RequestField.ofNullable("USER"))
                                                             .password(RequestField.ofNullable("PASS"))
                                                             .usageScope(RequestField.ofNull())
                                                             .build())
                                       .build(),
            MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);
    verify(settingsService, times(1)).updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);
    verify(settingServiceHelper, times(1))
        .updateEncryptedFieldsInResponse(isA(SettingAttribute.class), isA(Boolean.class));

    assertThat(payload.getCloudProvider()).isNotNull();
    assertThat(payload.getCloudProvider()).isInstanceOf(QLPcfCloudProvider.class);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateOfNonExistingSetting() {
    doReturn(null).when(settingsService).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);

    dataFetcher.mutateAndFetch(QLUpdateCloudProviderInput.builder()
                                   .cloudProviderId(CLOUD_PROVIDER_ID)
                                   .cloudProviderType(QLCloudProviderType.PCF)
                                   .pcfCloudProvider(QLPcfCloudProviderInput.builder()
                                                         .name(RequestField.ofNullable("NAME"))
                                                         .endpointUrl(RequestField.ofNullable("URL"))
                                                         .userName(RequestField.ofNullable("USER"))
                                                         .password(RequestField.ofNullable("PASS"))
                                                         .usageScope(RequestField.ofNull())
                                                         .build())
                                   .build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateWithoutIdParameter() {
    dataFetcher.mutateAndFetch(QLUpdateCloudProviderInput.builder().cloudProviderType(QLCloudProviderType.PCF).build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateWithoutTypeParameter() {
    dataFetcher.mutateAndFetch(QLUpdateCloudProviderInput.builder().cloudProviderId(CLOUD_PROVIDER_ID).build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());
  }
}
