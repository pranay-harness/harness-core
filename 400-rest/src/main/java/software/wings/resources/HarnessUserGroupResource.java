package software.wings.resources;

import io.harness.rest.RestResponse;

import software.wings.beans.security.HarnessUserGroup;
import software.wings.service.intfc.HarnessUserGroupService;

import com.google.inject.Singleton;
import io.swagger.annotations.Api;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Sets;

@Api("harnessUserGroup")
@Path("/harnessUserGroup")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
@Singleton
public class HarnessUserGroupResource {
  private final HarnessUserGroupService harnessUserGroupService;

  public HarnessUserGroupResource(HarnessUserGroupService harnessUserGroupService) {
    this.harnessUserGroupService = harnessUserGroupService;
  }

  @GET
  @Path("create")
  public RestResponse<HarnessUserGroup> createHarnessUserGroup(@QueryParam("name") String name,
      @QueryParam("description") String description, @QueryParam("memberIds") List<String> memberIds,
      @QueryParam("accountIds") List<String> accountIds) {
    HarnessUserGroup harnessUserGroup = harnessUserGroupService.createHarnessUserGroup(
        name, description, Sets.newHashSet(memberIds), Sets.newHashSet(accountIds), "RESTRICTED");

    return new RestResponse<>(harnessUserGroup);
  }

  @GET
  @Path("hello")
  public RestResponse<String> hello(){
    return new RestResponse<>("Hello");
  }
}
