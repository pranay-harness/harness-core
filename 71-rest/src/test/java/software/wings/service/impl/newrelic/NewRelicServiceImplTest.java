package software.wings.service.impl.newrelic;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.VerificationOperationException;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.newrelic.NewRelicService;

import java.io.IOException;

public class NewRelicServiceImplTest extends WingsBaseTest {
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private NewRelicDelegateService newRelicDelegateService;
  @Mock private SettingsService settingsService;

  @Inject private NewRelicService newRelicService;

  private SettingAttribute settingAttribute;
  private String settingId;
  private Long applicationId;

  private String connectorName = "new_relic";
  private String applicationName = "app_name";

  @Before
  public void setUp() throws IllegalAccessException {
    settingId = generateUuid();
    applicationId = 8L;

    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(newRelicService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(newRelicService, "settingsService", settingsService, true);

    NewRelicConfig newRelicConfig = NewRelicConfig.builder().newRelicUrl("url").build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withName(connectorName)
                                            .withUuid(settingId)
                                            .withValue(newRelicConfig)
                                            .build();
    when(settingsService.get(settingId)).thenReturn(settingAttribute);

    when(delegateProxyFactory.get(any(), any())).thenReturn(newRelicDelegateService);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testResolveApplicationName_correctAppName() throws IOException, CloneNotSupportedException {
    when(newRelicDelegateService.resolveNewRelicApplicationName(any(), any(), any(), any()))
        .thenReturn(NewRelicApplication.builder().id(applicationId).name(applicationName).build());
    NewRelicApplication newRelicApplication = newRelicService.resolveApplicationName(settingId, applicationName);
    assertThat(newRelicApplication).isNotNull();
    assertThat(newRelicApplication.getId()).isEqualTo(applicationId);
    assertThat(newRelicApplication.getName()).isEqualTo(applicationName);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testResolveApplicationName_delegateThrowsException() throws IOException, CloneNotSupportedException {
    when(newRelicDelegateService.resolveNewRelicApplicationName(any(), any(), any(), any()))
        .thenThrow(new DataCollectionException("Application name not found"));
    assertThatThrownBy(() -> newRelicService.resolveApplicationName(settingId, applicationName))
        .isInstanceOf(VerificationOperationException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testResolveApplicationId_correctArguments() throws IOException, CloneNotSupportedException {
    when(newRelicDelegateService.resolveNewRelicApplicationId(any(), any(), any(), any()))
        .thenReturn(NewRelicApplication.builder().id(applicationId).name(applicationName).build());
    NewRelicApplication newRelicApplication = newRelicService.resolveApplicationId(settingId, applicationId.toString());
    assertThat(newRelicApplication).isNotNull();
    assertThat(newRelicApplication.getId()).isEqualTo(applicationId);
    assertThat(newRelicApplication.getName()).isEqualTo(applicationName);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testResolveApplicationId_delegateThrowsException() throws IOException, CloneNotSupportedException {
    when(newRelicDelegateService.resolveNewRelicApplicationId(any(), any(), any(), any()))
        .thenThrow(new DataCollectionException("Application ID not found"));
    assertThatThrownBy(() -> newRelicService.resolveApplicationId(settingId, applicationId.toString()))
        .isInstanceOf(VerificationOperationException.class);
  }
}