package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.service.intf.CEYamlService;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.File;
import java.io.IOException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Api("yaml")
@Path("/yaml")
@Produces({MediaType.APPLICATION_JSON})
@NextGenManagerAuth
@Slf4j
@Service
@OwnedBy(CE)
public class CEYamlResource {
  @Inject CEYamlService ceYamlService;

  private static final String CONTENT_DISPOSITION = "Content-Disposition";
  private static final String ATTACHMENT_FILENAME = "attachment; filename=";
  public static final String YAML = ".yaml";

  @POST
  @Path("/generate-cost-optimisation-yaml")
  @ApiOperation(value = "Get Cost Optimisation Yaml", nickname = "getCostOptimisationYamlTemplate")
  public Response generateCostOptimisationYaml(@QueryParam("accountId") String accountId,
      @QueryParam("connectorIdentifier") String connectorIdentifier) throws IOException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      File yamlFile = ceYamlService.downloadCostOptimisationYaml(accountId, connectorIdentifier);
      return Response.ok(yamlFile)
          .header(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + "cost-optimisation-crd" + YAML)
          .build();
    }
  }
}
