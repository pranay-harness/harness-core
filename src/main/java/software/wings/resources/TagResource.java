package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.Tag;
import software.wings.beans.UuidList;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.TagService;

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

/**
 * Created by anubhaw on 4/25/16.
 */

@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
@Path("tags")
public class TagResource {
  @Inject private TagService tagService;

  @GET
  public RestResponse<PageResponse<Tag>> list(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @BeanParam PageRequest<Tag> request) {
    request.addFilter("appId", appId, EQ);
    request.addFilter("envId", envId, EQ);
    request.addFilter("rootTag", true, EQ);
    return new RestResponse<>(tagService.listRootTags(request));
  }

  @POST
  public RestResponse<Tag> save(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @QueryParam("parentTagId") String parentTagId, Tag tag) {
    tag.setEnvId(envId);
    tag.setAppId(appId);
    if (parentTagId == null || parentTagId.length() == 0) {
      tag.setRootTag(true);
    }
    return new RestResponse<>(tagService.createAndLinkTag(parentTagId, tag));
  }

  @GET
  @Path("{tagId}")
  public RestResponse<Tag> get(@QueryParam("appId") String appId, @PathParam("tagId") String tagId) {
    return new RestResponse<>(tagService.getTag(appId, tagId));
  }

  @PUT
  @Path("{tagId}")
  public RestResponse<Tag> update(@QueryParam("appId") String appId, @PathParam("tagId") String tagId, Tag tag) {
    tag.setAppId(appId);
    tag.setUuid(tagId);
    return new RestResponse<>(tagService.updateTag(tag));
  }

  @DELETE
  @Path("{tagId}")
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("tagId") String tagId) {
    tagService.deleteTag(appId, tagId);
    return new RestResponse();
  }

  @POST
  @Path("{tagId}/link/{childTag}")
  public RestResponse<Tag> linkTags(
      @QueryParam("appId") String appId, @PathParam("tagId") String tagId, @PathParam("childTag") String childTagId) {
    return new RestResponse<>(tagService.linkTags(appId, tagId, childTagId));
  }

  @POST
  @Path("{tagId}/tag-hosts")
  public RestResponse tagHosts(@QueryParam("appId") String appId, @PathParam("tagId") String tagId, UuidList uuidList) {
    tagService.tagHosts(appId, tagId, uuidList.getUuids());
    return new RestResponse();
  }

  @POST
  @Path("{tagId}/untag-hosts")
  public RestResponse untagHosts(
      @QueryParam("appId") String appId, @PathParam("tagId") String tagId, UuidList uuidList) {
    tagService.untagHosts(appId, tagId, uuidList.getUuids());
    return new RestResponse();
  }
}
