package io.harness.cvng.metrics.resources;

import io.harness.beans.ClientType;
import io.harness.cvng.metrics.services.api.MetricService;
import io.harness.security.annotations.HarnessApiKeyAuth;
import io.harness.security.annotations.PublicApi;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.io.IOException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

@Api("metrics")
@Path("/metrics")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
//@HarnessApiKeyAuth(clientTypes = ClientType.PROMETHEUS)
@PublicApi
@Slf4j
public class MetricResource {
  @Inject private MetricService metricService;

  @GET
  @Timed
  @ExceptionMetered
  public String get() throws IOException {
    return metricService.getRecordedMetricData();
  }
}