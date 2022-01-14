package io.harness.cvng.metrics.services.impl;

import static io.harness.rule.OwnerRule.KAMAL;

import static org.mockito.Matchers.eq;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.cdng.services.api.CVNGStepTaskService;
import io.harness.metrics.service.api.MetricService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class CVNGMetricsPublisherTest extends CvNextGenTestBase {
  @Inject private CVNGMetricsPublisher cvngMetricsPublisher;
  @Inject private CVNGStepTaskService cvngStepTaskService;
  @Mock private MetricService metricService;
  BuilderFactory builderFactory;
  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
    FieldUtils.writeField(cvngMetricsPublisher, "metricService", metricService, true);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testRecordMetrics() {
    CVNGStepTask cvngStepTask = builderFactory.cvngStepTaskBuilder().build();
    cvngStepTaskService.create(cvngStepTask);
    cvngMetricsPublisher.sendTaskStatusMetrics();
    Mockito.verify(metricService).recordMetric(eq("cvng_step_task_non_final_status_count"), eq(1.0));
    Mockito.verify(metricService).recordMetric(eq("cvng_step_task_in_progress_count"), eq(1.0));
  }
}