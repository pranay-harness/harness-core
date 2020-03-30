package software.wings.service.impl.dynatrace;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.service.impl.analysis.APMDelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.dynatrace.DynaTraceService;
import software.wings.service.intfc.security.SecretManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Praveen
 */
public class DynatraceServiceTest {
  @Mock private SettingsService mockSettingsService;
  @Mock private DelegateProxyFactory mockDelegateProxyFactory;
  @Mock private APMDelegateService apmDelegateService;
  @Mock private SecretManager mockSecretManager;

  @InjectMocks DynaTraceService service = new DynaTraceServiceImpl();

  private String dynatraceSettingId;

  @Before
  public void setup() throws IllegalAccessException {
    dynatraceSettingId = generateUuid();
    MockitoAnnotations.initMocks(this);

    DynaTraceConfig dynaTraceConfig =
        DynaTraceConfig.builder().dynaTraceUrl("dummyURL").apiToken("apiToken".toCharArray()).build();
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(dynaTraceConfig);

    when(mockSettingsService.get(dynatraceSettingId)).thenReturn(attribute);
    when(mockDelegateProxyFactory.get(any(), any())).thenReturn(apmDelegateService);
    when(apmDelegateService.fetch(any(), any())).thenReturn(JsonUtils.asJson(getDynatraceServiceList()));
  }

  private List<DynaTraceApplication> getDynatraceServiceList() {
    List<DynaTraceApplication> services = new ArrayList<>();
    services.add(DynaTraceApplication.builder().displayName("serviceName1").entityId("entityID1").build());
    services.add(DynaTraceApplication.builder().displayName("serviceName2").entityId("entityID2").build());
    services.add(DynaTraceApplication.builder().displayName("serviceName3").entityId("entityID3").build());
    return services;
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetServices_happyCase() {
    List<DynaTraceApplication> serviceList = service.getServices(dynatraceSettingId);
    assertThat(serviceList).isNotEmpty();
    assertThat(serviceList.containsAll(getDynatraceServiceList())).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetServices_emptySettingId() {
    List<DynaTraceApplication> serviceList = service.getServices(null);
    assertThat(serviceList).isNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testResolveServiceNameToID_happyCase() {
    String entityID = service.resolveDynatraceServiceNameToId(dynatraceSettingId, "serviceName3");
    assertThat(entityID).isEqualTo("entityID3");
  }

  @Test(expected = DataCollectionException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testResolveServiceNameToID_badServiceName() {
    String entityID = service.resolveDynatraceServiceNameToId(dynatraceSettingId, "serviceName5");
    assertThat(entityID).isNull();
  }

  @Test(expected = DataCollectionException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testResolveServiceNameToID_badSettingId() {
    String entityID = service.resolveDynatraceServiceNameToId(null, "serviceName5");
    assertThat(entityID).isNull();
  }

  @Test(expected = DataCollectionException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testResolveServiceNameToID_emptyName() {
    String entityID = service.resolveDynatraceServiceNameToId(dynatraceSettingId, "");
    assertThat(entityID).isNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testValidateServiceID_happyCase() {
    boolean isValid = service.validateDynatraceServiceId(dynatraceSettingId, "entityID3");
    assertThat(isValid).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testValidateServiceID_invalidID() {
    boolean isValid = service.validateDynatraceServiceId(dynatraceSettingId, "entityID13");
    assertThat(isValid).isFalse();
  }
}
