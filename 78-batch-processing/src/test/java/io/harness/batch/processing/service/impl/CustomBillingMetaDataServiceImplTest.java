package io.harness.batch.processing.service.impl;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.batch.processing.pricing.data.VMInstanceBillingData;
import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryHelperService;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.entities.BillingDataPipelineRecord;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.settings.SettingValue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class CustomBillingMetaDataServiceImplTest extends CategoryTest {
  @Mock private BigQueryHelperService bigQueryHelperService;
  @Mock private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Mock private BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  @InjectMocks private CustomBillingMetaDataServiceImpl customBillingMetaDataService;

  private static final String ACCOUNT_ID = "zEaak-FLS425IEO7OLzMUg";
  private static final String RESOURCE_ID = "resourceId";
  private static final String SETTING_ID = "settingID";
  private static final String AWS_DATA_SETID = "dataSetId";

  private final Instant NOW = Instant.now().truncatedTo(ChronoUnit.HOURS);
  private final Instant START_TIME = NOW.minus(1, ChronoUnit.HOURS);
  private final Instant END_TIME = NOW;

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetAwsDataSetId() {
    when(cloudToHarnessMappingService.listSettingAttributesCreatedInDuration(
             ACCOUNT_ID, SettingAttribute.SettingCategory.CE_CONNECTOR, SettingValue.SettingVariableTypes.CE_AWS))
        .thenReturn(ceConnectorSettingAttribute());
    when(billingDataPipelineRecordDao.getBySettingId(ACCOUNT_ID, SETTING_ID))
        .thenReturn(BillingDataPipelineRecord.builder().dataSetId(AWS_DATA_SETID).build());
    String awsDataSetId = customBillingMetaDataService.getAwsDataSetId(ACCOUNT_ID);
    assertThat(awsDataSetId).isEqualTo(AWS_DATA_SETID);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetPipelineJobStatusWhenJobNotFinished() {
    when(cloudToHarnessMappingService.listSettingAttributesCreatedInDuration(
             ACCOUNT_ID, SettingAttribute.SettingCategory.CE_CONNECTOR, SettingValue.SettingVariableTypes.CE_AWS))
        .thenReturn(ceConnectorSettingAttribute());
    when(billingDataPipelineRecordDao.getBySettingId(ACCOUNT_ID, SETTING_ID))
        .thenReturn(BillingDataPipelineRecord.builder().dataSetId(AWS_DATA_SETID).build());
    when(bigQueryHelperService.getAwsBillingData(START_TIME, END_TIME, AWS_DATA_SETID)).thenReturn(null);
    Boolean jobFinished = customBillingMetaDataService.checkPipelineJobFinished(ACCOUNT_ID, START_TIME, END_TIME);
    assertThat(jobFinished).isFalse();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetPipelineJobStatusWhenJobFinished() {
    Map<String, VMInstanceBillingData> vmInstanceBillingDataMap = new HashMap<>();
    VMInstanceBillingData vmInstanceBillingData =
        VMInstanceBillingData.builder().computeCost(10).networkCost(0).resourceId(RESOURCE_ID).build();
    vmInstanceBillingDataMap.put(RESOURCE_ID, vmInstanceBillingData);
    when(cloudToHarnessMappingService.listSettingAttributesCreatedInDuration(
             ACCOUNT_ID, SettingAttribute.SettingCategory.CE_CONNECTOR, SettingValue.SettingVariableTypes.CE_AWS))
        .thenReturn(ceConnectorSettingAttribute());
    when(billingDataPipelineRecordDao.getBySettingId(ACCOUNT_ID, SETTING_ID))
        .thenReturn(BillingDataPipelineRecord.builder().dataSetId(AWS_DATA_SETID).build());
    when(bigQueryHelperService.getAwsBillingData(START_TIME, END_TIME, AWS_DATA_SETID))
        .thenReturn(vmInstanceBillingDataMap);
    Boolean jobFinished = customBillingMetaDataService.checkPipelineJobFinished(ACCOUNT_ID, START_TIME, END_TIME);
    assertThat(jobFinished).isTrue();
  }

  private List<SettingAttribute> ceConnectorSettingAttribute() {
    CEAwsConfig ceAwsConfig = CEAwsConfig.builder().build();
    return Collections.singletonList(settingAttribute(ceAwsConfig));
  }

  private SettingAttribute settingAttribute(SettingValue settingValue) {
    SettingAttribute settingAttribute = new SettingAttribute();
    settingAttribute.setUuid(SETTING_ID);
    settingAttribute.setValue(settingValue);
    settingAttribute.setCategory(SettingAttribute.SettingCategory.CE_CONNECTOR);
    return settingAttribute;
  }
}
