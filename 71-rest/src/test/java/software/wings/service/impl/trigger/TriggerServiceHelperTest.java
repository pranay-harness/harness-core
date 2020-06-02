package software.wings.service.impl.trigger;

import static io.harness.rule.OwnerRule.AADITI;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;
import static software.wings.beans.trigger.ArtifactSelection.Type.WEBHOOK_VARIABLE;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildNexusArtifactStream;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildWorkflowWebhookTrigger;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.TRIGGER_ID;

import com.google.gson.Gson;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.beans.WebHookToken;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.Trigger;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;

import java.util.HashMap;
import java.util.Map;

public class TriggerServiceHelperTest extends WingsBaseTest {
  private static final String ARTIFACT_STREAM_ID_1 = "ARTIFACT_STREAM_ID_1";
  private static final String CATALOG_SERVICE_NAME = "Catalog";
  @Inject @InjectMocks private TriggerServiceHelper triggerServiceHelper;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private ArtifactStreamService artifactStreamService;
  Trigger workflowWebhookConditionTrigger = buildWorkflowWebhookTrigger();

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void constructWebhookTokenForParameterizedArtifactStream() {
    when(featureFlagService.isEnabled(FeatureName.NAS_SUPPORT, ACCOUNT_ID)).thenReturn(true);
    Mockito.when(artifactStreamService.get(ARTIFACT_STREAM_ID_1)).thenReturn(buildNexusArtifactStream());

    workflowWebhookConditionTrigger.setArtifactSelections(asList(ArtifactSelection.builder()
                                                                     .type(WEBHOOK_VARIABLE)
                                                                     .serviceId(SERVICE_ID)
                                                                     .artifactStreamId(ARTIFACT_STREAM_ID_1)
                                                                     .build()));
    workflowWebhookConditionTrigger.setUuid(TRIGGER_ID);
    Map<String, String> parameters = new HashMap<>();
    parameters.put("MyVar", "MyVar_placeholder");
    WebHookToken webHookToken = triggerServiceHelper.constructWebhookToken(workflowWebhookConditionTrigger, null,
        asList(Service.builder().uuid(SERVICE_ID).name(CATALOG_SERVICE_NAME).accountId(ACCOUNT_ID).build()), true,
        parameters);
    assertThat(webHookToken).isNotNull();
    assertThat(webHookToken.getWebHookToken()).isNotNull();
    assertThat(webHookToken.getPayload()).isNotEmpty();

    HashMap<String, Object> hashMap = new Gson().fromJson(webHookToken.getPayload(), HashMap.class);
    assertThat(hashMap).containsKeys("application", "artifacts", "parameters");
    assertThat(hashMap.get("application")).isEqualTo(APP_ID);
    assertThat(hashMap.get("artifacts"))
        .isNotNull()
        .toString()
        .contains(
            "[{service=Catalog, buildNumber=Catalog_BUILD_NUMBER_PLACE_HOLDER, artifactVariables={path=PATH_PLACE_HOLDER, groupId=GROUPID_PLACE_HOLDER}}]");
    assertThat(hashMap.get("parameters")).isNotNull().toString().contains("MyVar=MyVar_placeholder");
  }
}
