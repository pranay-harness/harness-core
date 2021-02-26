package io.harness.resource;

import io.harness.Tester;
import io.harness.beans.SampleBean;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("test")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
public class TestResource {
  @Inject Tester tester;

  @POST
  public ResponseDTO<SampleBean> get(@QueryParam("data") String stringToSave) {
    final SampleBean save = tester.save(stringToSave);
    return ResponseDTO.newResponse(save);
  }
}
