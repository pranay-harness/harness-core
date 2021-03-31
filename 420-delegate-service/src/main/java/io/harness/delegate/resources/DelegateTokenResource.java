package io.harness.delegate.resources;

import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;
import io.harness.service.intfc.DelegateTokenService;

import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import lombok.extern.slf4j.Slf4j;

@Api("/delegate-token")
@Path("/delegate-token")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateTokenResource {
  private final DelegateTokenService delegateTokenService;

  @Inject
  public DelegateTokenResource(DelegateTokenService delegateTokenService) {
    this.delegateTokenService = delegateTokenService;
  }

  @DelegateAuth
  @GET
  @Path("dummy")
  @Timed
  @ExceptionMetered
  public RestResponse<String> getDummy() {
    log.info("Dummy endpoint!");
    return new RestResponse<>("Dummy endpoint!");
  }
}
