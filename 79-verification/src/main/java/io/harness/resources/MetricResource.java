package io.harness.resources;

import static software.wings.common.VerificationConstants.VERIFICATION_SERVICE_METRICS;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.metrics.HarnessMetricRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.HarnessApiKey.ClientType;
import software.wings.security.annotations.HarnessApiKeyAuth;

import java.io.IOException;
import java.io.StringWriter;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Rest End point to expose all the metrics in HarnessMetricRegistry
 * This API is used by Prometheus to scape all the metrics
 *
 * Created by Pranjal on 11/02/2018
 */
@Api("metrics")
@Path("/metrics")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@HarnessApiKeyAuth(clientTypes = ClientType.PROMETHEUS)
@Slf4j
public class MetricResource {
  @Inject private HarnessMetricRegistry metricRegistry;

  @GET
  @Timed
  @ExceptionMetered
  public String get() throws IOException {
    final StringWriter writer = new StringWriter();
    try {
      TextFormat.write004(writer, metricRegistry.getMetric(VERIFICATION_SERVICE_METRICS));
      writer.flush();
    } finally {
      writer.close();
    }
    return writer.getBuffer().toString();
  }
}
