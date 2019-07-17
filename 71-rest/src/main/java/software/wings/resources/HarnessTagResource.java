package software.wings.resources;

import static io.harness.beans.SearchFilter.Operator.IN;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.HarnessTagLink.HarnessTagLinkKeys;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.HarnessTagService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("tags")
@Path("tags")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@AuthRule(permissionType = PermissionType.LOGGED_IN)
public class HarnessTagResource {
  @Inject HarnessTagService harnessTagService;

  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.TAG_MANAGEMENT)
  public RestResponse<HarnessTag> create(@QueryParam("accountId") String accountId, HarnessTag tag) {
    tag.setAccountId(accountId);
    return new RestResponse<>(harnessTagService.create(tag));
  }

  @PUT
  @Path("{key}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.TAG_MANAGEMENT)
  public RestResponse<HarnessTag> update(
      @QueryParam("accountId") String accountId, @PathParam("key") String key, HarnessTag tag) {
    tag.setAccountId(accountId);
    tag.setKey(key);
    return new RestResponse<>(harnessTagService.update(tag));
  }

  @GET
  @Path("{key}")
  @Timed
  @ExceptionMetered
  public RestResponse<HarnessTag> get(@QueryParam("accountId") String accountId, @PathParam("key") String key,
      @QueryParam("includeInUseValues") boolean includeInUseValues) {
    if (includeInUseValues) {
      return new RestResponse<>(harnessTagService.getTagWithInUseValues(accountId, key));
    } else {
      return new RestResponse<>(harnessTagService.get(accountId, key));
    }
  }

  @DELETE
  @Path("{key}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.TAG_MANAGEMENT)
  public RestResponse delete(@QueryParam("accountId") String accountId, @PathParam("key") String key) {
    harnessTagService.delete(accountId, key);
    return new RestResponse();
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<HarnessTag>> list(
      @QueryParam("accountId") String accountId, @BeanParam PageRequest<HarnessTag> request) {
    request.addFilter(HarnessTagLinkKeys.accountId, IN, accountId);
    return new RestResponse<>(harnessTagService.list(request));
  }

  @POST
  @Path("attach")
  @Timed
  @ExceptionMetered
  public RestResponse attachTag(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, HarnessTagLink tagLink) {
    tagLink.setAccountId(accountId);
    harnessTagService.authorizeTagAttachDetach(appId, tagLink);
    harnessTagService.attachTag(tagLink);
    return new RestResponse();
  }

  @POST
  @Path("detach")
  @Timed
  @ExceptionMetered
  public RestResponse detachTag(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, HarnessTagLink tagLink) {
    harnessTagService.authorizeTagAttachDetach(appId, tagLink);
    harnessTagService.detachTag(accountId, tagLink.getEntityId(), tagLink.getKey());
    return new RestResponse();
  }

  @GET
  @Path("links")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<HarnessTagLink>> listResourcesWithTag(
      @QueryParam("accountId") String accountId, @BeanParam PageRequest<HarnessTagLink> request) {
    request.addFilter(HarnessTagLinkKeys.accountId, IN, accountId);
    return new RestResponse<>(harnessTagService.listResourcesWithTag(accountId, request));
  }
}
