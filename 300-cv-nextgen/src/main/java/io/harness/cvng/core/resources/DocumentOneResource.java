package io.harness.cvng.core.resources;

import io.harness.cvng.core.services.api.DocumentOneService;
import io.harness.repositories.DocumentOne;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import retrofit2.http.Body;

@Api("document-one")
@Path("document-one")
@Produces("application/json")
@NextGenManagerAuth
public class DocumentOneResource {
  @Inject private DocumentOneService documentOneService;

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "saves data", nickname = "saveDocumentOne")
  public RestResponse<Void> save(@NotNull @Valid @Body DocumentOne documentOne) {
    documentOneService.save(documentOne);
    return new RestResponse<>(null);
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "saves data", nickname = "getDocumentOne")
  public RestResponse<List<DocumentOne>> get() {
    return new RestResponse<>(documentOneService.get());
  }
}
