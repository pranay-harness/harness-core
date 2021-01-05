package io.harness.app.impl;

import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.getPipeline;
import static io.harness.rule.OwnerRule.HARSH;

import static org.joor.Reflect.on;

import io.harness.app.resources.CIWebhookTriggerResource;
import io.harness.category.element.UnitTests;
import io.harness.core.trigger.WebhookTriggerProcessor;
import io.harness.core.trigger.WebhookTriggerProcessorUtils;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.io.IOException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class WebhookExecutionTest extends CIManagerTest {
  @Mock private NGPipelineService ngPipelineService;
  @Mock private CIPipelineExecutionService ciPipelineExecutionService;
  @Mock private WebhookTriggerProcessorUtils webhookTriggerProcessorUtils;
  @Inject private WebhookTriggerProcessor webhookTriggerProcessor;
  @Mock HttpHeaders httpHeaders;

  @InjectMocks CIBuildInfoServiceImpl ciBuildInfoService;
  @Inject private CIWebhookTriggerResource webhookTriggerResource;

  public static final String X_GIT_HUB_EVENT = "X-GitHub-Event";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    on(webhookTriggerResource).set("webhookTriggerProcessor", webhookTriggerProcessor);
    on(webhookTriggerResource).set("ngPipelineService", ngPipelineService);
    on(webhookTriggerResource).set("ciPipelineExecutionService", ciPipelineExecutionService);
    on(webhookTriggerProcessor).set("webhookTriggerProcessorUtils", webhookTriggerProcessorUtils);
  }
  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldTestJsonPullWithBranchName() throws IOException {
    MultivaluedMap<String, String> headersMultiMap = new MultivaluedHashMap<>();
    headersMultiMap.add(X_GIT_HUB_EVENT, "push");
    ClassLoader classLoader = getClass().getClassLoader();
    NgPipelineEntity ngPipelineEntity = getPipeline();
    //    File file = new File(classLoader.getResource("github_pull_request.json").getFile());
    //    when(ngPipelineService.getPipeline(PIPELINE_ID)).thenReturn(ngPipelineEntity);
    //    when(httpHeaders.getRequestHeaders()).thenReturn(headersMultiMap);
    //    when(httpHeaders.getHeaderString(X_GIT_HUB_EVENT)).thenReturn("push");
    //
    //    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());
    //    webhookTriggerResource.runPipelineFromTrigger(PIPELINE_ID, payLoad, httpHeaders);
    // verify(ciPipelineExecutionService, times(1)).executePipeline(any(), any(), any());
  }
}
