package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.stackdriver.StackDriverLogDataCollectionInfo;
import software.wings.service.intfc.SettingsService;

public class AbstractLogAnalysisStateTest extends WingsBaseTest {
  @Mock private SettingsService settingsService;

  private StackDriverLogState stackDriverLogState;
  private GcpConfig gcpConfig;
  private AnalysisContext analysisContext;
  private SettingAttribute settingAttribute;
  private String accountId;
  private String appId;
  private String configId;

  @Before
  public void setUp() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);

    accountId = generateUuid();
    appId = generateUuid();
    configId = generateUuid();
    stackDriverLogState = Mockito.spy(new StackDriverLogState("stackDriverLog"));
    analysisContext = AnalysisContext.builder().appId(appId).accountId(accountId).query("query").build();
    gcpConfig = GcpConfig.builder().accountId(accountId).build();
    settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                           .withAccountId(accountId)
                           .withAppId(appId)
                           .withValue(gcpConfig)
                           .build();

    FieldUtils.writeField(stackDriverLogState, "settingsService", settingsService, true);

    when(settingsService.get(configId)).thenReturn(settingAttribute);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCreateDataCollectionInfo_stackDriverLog() {
    doReturn(configId).when(stackDriverLogState).getResolvedConnectorId(any(), any(), any());
    doReturn("query").when(stackDriverLogState).getRenderedQuery();
    StackDriverLogDataCollectionInfo stackDriverLogDataCollectionInfo =
        (StackDriverLogDataCollectionInfo) stackDriverLogState.createDataCollectionInfo(analysisContext, null);

    assertThat(stackDriverLogDataCollectionInfo.getAccountId()).isEqualTo(accountId);
    assertThat(stackDriverLogDataCollectionInfo.getApplicationId()).isEqualTo(appId);
    assertThat(stackDriverLogDataCollectionInfo.getQuery()).isEqualTo(analysisContext.getQuery());
    assertThat(stackDriverLogDataCollectionInfo.getGcpConfig()).isEqualTo(gcpConfig);
  }
}