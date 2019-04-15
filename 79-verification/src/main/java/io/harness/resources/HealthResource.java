package io.harness.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.security.annotations.PublicApi;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by Praveen
 */

@Api("health")
@Path("/health")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PublicApi
@Slf4j
public class HealthResource {
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<String> get() {
    return new RestResponse<>("healthy");
  }
}
