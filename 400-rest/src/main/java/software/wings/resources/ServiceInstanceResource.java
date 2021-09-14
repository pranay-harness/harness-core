/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;

import software.wings.beans.ServiceInstance;
import software.wings.service.intfc.ServiceInstanceService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Created by anubhaw on 5/26/16.
 */
@Api("/service-instances")
@Path("service-instances")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ServiceInstanceResource {
  /**
   * The Instance service.
   */
  @Inject ServiceInstanceService instanceService;

  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<ServiceInstance>> list(@BeanParam PageRequest<ServiceInstance> pageRequest) {
    return new RestResponse<>(instanceService.list(pageRequest));
  }
}
