package io.harness.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.registry.HarnessMetricRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.security.annotations.PublicApi;

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
@PublicApi
public class MetricResource {
  private static final Logger logger = LoggerFactory.getLogger(HealthResource.class);

  @Inject private HarnessMetricRegistry metricRegistry;

  @GET
  @Timed
  @ExceptionMetered
  public String get() throws IOException {
    final StringWriter writer = new StringWriter();
    try {
      TextFormat.write004(writer, metricRegistry.getMetric());
      writer.flush();
    } finally {
      writer.close();
    }
    return writer.getBuffer().toString();
  }
}
