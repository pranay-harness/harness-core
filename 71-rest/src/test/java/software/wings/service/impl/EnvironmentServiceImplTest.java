package software.wings.service.impl;

import static io.harness.rule.OwnerRule.RAMA;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;

import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.FeatureName;
import software.wings.beans.HarnessTagLink;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.InfrastructureDefinitionService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvironmentServiceImplTest extends WingsBaseTest {
  @Mock FeatureFlagService featureFlagService;
  @Mock AppService appService;
  @Mock InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private HarnessTagService harnessTagService;
  @InjectMocks @Inject EnvironmentServiceImpl environmentService;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldAddInfraDefWithCount() {
    doReturn(ACCOUNT_ID).when(appService).getAccountIdByAppId(APP_ID);
    doReturn(true).when(featureFlagService).isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID);
    PageResponse<Environment> environmentPageResponse = new PageResponse<>();
    Environment env1 = Environment.Builder.anEnvironment().uuid("ENV_1").build();
    Environment env2 = Environment.Builder.anEnvironment().uuid("ENV_2").build();
    InfrastructureDefinition infra1 = InfrastructureDefinition.builder().uuid("ID1").build();
    InfrastructureDefinition infra2 = InfrastructureDefinition.builder().uuid("ID2").build();
    environmentPageResponse.setResponse(Arrays.asList(env1, env2));
    doReturn(Arrays.asList(infra1, infra2))
        .when(infrastructureDefinitionService)
        .getNameAndIdForEnvironment(APP_ID, "ENV_1", 5);
    doReturn(Collections.emptyList())
        .when(infrastructureDefinitionService)
        .getNameAndIdForEnvironment(APP_ID, "ENV_2", 5);
    Map<String, Integer> envIdCountMap = new HashMap<>();
    envIdCountMap.put("ENV_1", 2);
    envIdCountMap.put("ENV_2", 0);
    doReturn(envIdCountMap)
        .when(infrastructureDefinitionService)
        .getCountForEnvironments(APP_ID, Arrays.asList("ENV_1", "ENV_2"));

    environmentService.addInfraDefDetailToEnv(APP_ID, environmentPageResponse);

    env1 = environmentPageResponse.getResponse().get(0);
    env2 = environmentPageResponse.getResponse().get(1);
    assertThat(env1.getInfrastructureDefinitions()).hasSize(2);
    assertThat(env2.getInfrastructureDefinitions()).hasSize(0);
    assertThat(env1.getInfraDefinitionsCount()).isEqualTo(2);
    assertThat(env2.getInfraDefinitionsCount()).isEqualTo(0);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldNotAddWhenFeatureFlagDisabled() {
    doReturn(ACCOUNT_ID).when(appService).getAccountIdByAppId(APP_ID);
    doReturn(false).when(featureFlagService).isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID);
    PageResponse<Environment> environmentPageResponse = new PageResponse<>();
    environmentPageResponse.setResponse(Collections.singletonList(Environment.Builder.anEnvironment().build()));

    environmentService.addInfraDefDetailToEnv(APP_ID, environmentPageResponse);

    Environment environment = environmentPageResponse.getResponse().get(0);
    assertThat(environment.getInfrastructureDefinitions()).isNull();
    assertThat(environment.getInfraDefinitionsCount()).isEqualTo(0);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldSetServiceDeploymentTypeAndArtifactTypeTag() {
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(featureFlagService.isEnabled(FeatureName.HARNESS_TAGS, ACCOUNT_ID)).thenReturn(true);
    Environment env1 = Environment.Builder.anEnvironment()
                           .uuid(ENV_ID)
                           .name(ENV_NAME)
                           .appId(APP_ID)
                           .environmentType(EnvironmentType.PROD)
                           .build();
    Environment savedEnv = environmentService.save(env1);
    List<HarnessTagLink> tagLinksWithEntityId =
        harnessTagService.getTagLinksWithEntityId(ACCOUNT_ID, savedEnv.getUuid());
    assertThat(tagLinksWithEntityId).hasSize(1);
    assertTrue(tagLinksWithEntityId.stream().anyMatch(
        tagLink -> tagLink.getKey().equals("environmentType") && tagLink.getValue().equals("PROD")));
  }
}