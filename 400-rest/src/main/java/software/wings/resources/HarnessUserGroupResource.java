package software.wings.resources;

import io.harness.rest.RestResponse;

import software.wings.beans.security.HarnessUserGroup;
import software.wings.service.intfc.HarnessUserGroupService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.annotations.Api;
import java.util.Arrays;
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

  @Inject
  public HarnessUserGroupResource(HarnessUserGroupService harnessUserGroupService) {
    this.harnessUserGroupService = harnessUserGroupService;
  }

  @GET
  @Path("create")
  public RestResponse<HarnessUserGroup> createHarnessUserGroup(@QueryParam("accountId") String accountId,
      @QueryParam("name") String name, @QueryParam("description") String description,
      @QueryParam("memberIds") List<String> memberIds) {
    HarnessUserGroup harnessUserGroup = harnessUserGroupService.createHarnessUserGroup(name, description,
        Sets.newHashSet(memberIds), Sets.newHashSet(Arrays.asList(accountId)), HarnessUserGroup.GroupType.RESTRICTED);
    return new RestResponse<>(harnessUserGroup);
  }

  @GET
  @Path("delete")
  public RestResponse<Boolean> deleteHarnessUserGroup(
      @QueryParam("accountId") String acccountId, @QueryParam("harnessUserGroupId") String harnessUserGroupId) {
    return new RestResponse<>(harnessUserGroupService.delete(harnessUserGroupId));
  }

  @GET
  @Path("updateMembers")
  public RestResponse<HarnessUserGroup> updateHarnessUserGroupMembers(@QueryParam("accounId") String accountId,
      @QueryParam("harnessUserGroupId") String harnessUserGroupId, @QueryParam("memberIds") List<String> memberIds) {
    return new RestResponse<>(harnessUserGroupService.updateMembers(harnessUserGroupId, Sets.newHashSet(memberIds)));
  }

  @GET
  @Path("listHarnessUserGroupForAccount")
  public RestResponse<List<HarnessUserGroup>> listHarnessUserGroup(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(harnessUserGroupService.listHarnessUserGroupForAccount(accountId));
  }
}
