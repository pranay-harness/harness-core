package io.harness.app.impl;

import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.PIPELINE_ID;
import static io.harness.app.impl.CIBuildInfoServiceImplTestHelper.getPipeline;
import static io.harness.rule.OwnerRule.HARSH;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.service.impl.WebHookServiceImpl.X_GIT_HUB_EVENT;

import com.google.inject.Inject;

import io.harness.app.intfc.CIPipelineService;
import io.harness.app.resources.CIWebhookTriggerResource;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import io.harness.core.trigger.WebhookTriggerProcessor;
import io.harness.core.trigger.WebhookTriggerProcessorUtils;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.rule.Owner;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

public class WebhookExecutionTest extends CIManagerTest {
  @Mock private CIPipelineService ciPipelineService;
  @Mock private CIPipelineExecutionService ciPipelineExecutionService;
  @Mock private WebhookTriggerProcessorUtils webhookTriggerProcessorUtils;
  @Inject private WebhookTriggerProcessor webhookTriggerProcessor;
  @Mock HttpHeaders httpHeaders;

  @InjectMocks CIBuildInfoServiceImpl ciBuildInfoService;
  @Inject private CIWebhookTriggerResource webhookTriggerResource;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    on(webhookTriggerResource).set("webhookTriggerProcessor", webhookTriggerProcessor);
    on(webhookTriggerResource).set("ciPipelineService", ciPipelineService);
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
    File file = new File(classLoader.getResource("github_pull_request.json").getFile());
    when(ciPipelineService.readPipeline(PIPELINE_ID)).thenReturn(ngPipelineEntity);
    when(httpHeaders.getRequestHeaders()).thenReturn(headersMultiMap);
    when(httpHeaders.getHeaderString(X_GIT_HUB_EVENT)).thenReturn("push");

    String payLoad = FileUtils.readFileToString(file, Charset.defaultCharset());
    webhookTriggerResource.runPipelineFromTrigger(PIPELINE_ID, payLoad, httpHeaders);
    verify(ciPipelineExecutionService, times(1)).executePipeline(any(), any(), any());
  }
}