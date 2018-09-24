package software.wings.yaml.handler.connectors.configyamlhandlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import io.harness.beans.PageResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.setting.loadbalancer.ElasticLoadBalancerConfigYamlHandler;
import software.wings.service.intfc.InfrastructureMappingService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ElasticLoadBalancerConfigYamlHandlerTest extends BaseSettingValueConfigYamlHandlerTest {
  @InjectMocks @Inject private ElasticLoadBalancerConfigYamlHandler yamlHandler;
  @Mock InfrastructureMappingService infrastructureMappingService;
  @Mock InfrastructureMapping infrastructureMapping;

  public static final String loadBalancerName = "loadBalancer";

  private String invalidYamlContent = "region_invalid: eu-central-1\n"
      + "loadBalancerName: jk\n"
      + "accessKey: AKIAIKL7FYYF2TIYHCLQ\n"
      + "secretKey: safeharness:j6I-dh8xSSurpME_loAslQ\n"
      + "harnessApiVersion: '1.0'\n"
      + "type: ELB";

  private Class yamlClass = ElasticLoadBalancerConfig.Yaml.class;

  @Before
  public void setUp() throws HarnessException, IOException {
    List<InfrastructureMapping> list = new ArrayList<>();
    PageResponse<InfrastructureMapping> pageResponse = new PageResponse<>();
    pageResponse.setResponse(list);
    doReturn(pageResponse).when(infrastructureMappingService).list(any(), any());
  }

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    String name = "ELB" + System.currentTimeMillis();

    // 1. Create ELB config
    SettingAttribute settingAttributeSaved = createELBVerificationProvider(name);
    assertEquals(name, settingAttributeSaved.getName());

    testCRUD(generateSettingValueYamlConfig(name, settingAttributeSaved));
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    String name = "ELB" + System.currentTimeMillis();

    // 1. Create ELB config record
    SettingAttribute settingAttributeSaved = createELBVerificationProvider(name);
    testFailureScenario(generateSettingValueYamlConfig(name, settingAttributeSaved));
  }

  private SettingAttribute createELBVerificationProvider(String name) {
    // Generate appdynamics verification connector
    when(settingValidationService.validate(any(SettingAttribute.class))).thenReturn(true);

    return settingsService.save(aSettingAttribute()
                                    .withCategory(Category.CONNECTOR)
                                    .withName(name)
                                    .withAccountId(ACCOUNT_ID)
                                    .withValue(ElasticLoadBalancerConfig.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .region(Regions.CA_CENTRAL_1)
                                                   .loadBalancerName(loadBalancerName)
                                                   .accessKey(accesskey)
                                                   .secretKey(token.toCharArray())
                                                   .build())
                                    .build());
  }

  private SettingValueYamlConfig generateSettingValueYamlConfig(String name, SettingAttribute settingAttributeSaved) {
    return SettingValueYamlConfig.builder()
        .yamlHandler(yamlHandler)
        .yamlClass(yamlClass)
        .settingAttributeSaved(settingAttributeSaved)
        .yamlDirPath(loadBalancerYamlDir)
        .invalidYamlContent(invalidYamlContent)
        .name(name)
        .configclazz(ElasticLoadBalancerConfig.class)
        .updateMethodName("setLoadBalancerName")
        .currentFieldValue(loadBalancerName)
        .build();
  }
}
