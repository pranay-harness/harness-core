package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.Tag;
import software.wings.beans.UuidList;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
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

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/25/16.
 */
@Api("tags")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
@Path("tags")
public class TagResource {
  @Inject private TagService tagService;

  /**
   * List.
   *
   * @param appId   the app id
   * @param envId   the env id
   * @param request the request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<Tag>> list(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @BeanParam PageRequest<Tag> request) {
    request.addFilter("appId", appId, EQ);
    request.addFilter("envId", envId, EQ);
    request.addFilter("rootTag", true, EQ);
    return new RestResponse<>(tagService.listRootTags(request));
  }

  /**
   * Save.
   *
   * @param appId       the app id
   * @param envId       the env id
   * @param parentTagId the parent tag id
   * @param tag         the tag
   * @return the rest response
   */
  @POST
  public RestResponse<Tag> save(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @QueryParam("parentTagId") String parentTagId, Tag tag) {
    tag.setEnvId(envId);
    tag.setAppId(appId);
    return new RestResponse<>(tagService.saveTag(parentTagId, tag));
  }

  /**
   * Gets the.
   *
   * @param appId the app id
   * @param tagId the tag id
   * @return the rest response
   */
  @GET
  @Path("{tagId}")
  public RestResponse<Tag> get(@QueryParam("appId") String appId, @PathParam("tagId") String tagId) {
    return new RestResponse<>(tagService.getTag(appId, tagId));
  }

  /**
   * Update.
   *
   * @param appId the app id
   * @param tagId the tag id
   * @param tag   the tag
   * @return the rest response
   */
  @PUT
  @Path("{tagId}")
  public RestResponse<Tag> update(@QueryParam("appId") String appId, @PathParam("tagId") String tagId, Tag tag) {
    tag.setAppId(appId);
    tag.setUuid(tagId);
    return new RestResponse<>(tagService.updateTag(tag));
  }

  /**
   * Delete.
   *
   * @param appId the app id
   * @param tagId the tag id
   * @return the rest response
   */
  @DELETE
  @Path("{tagId}")
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("tagId") String tagId) {
    tagService.deleteTag(appId, tagId);
    return new RestResponse();
  }

  /**
   * Tag hosts.
   *
   * @param appId    the app id
   * @param tagId    the tag id
   * @param uuidList the uuid list
   * @return the rest response
   */
  @POST
  @Path("{tagId}/tag-hosts")
  public RestResponse tagHosts(@QueryParam("appId") String appId, @PathParam("tagId") String tagId, UuidList uuidList) {
    tagService.tagHosts(appId, tagId, uuidList.getUuids());
    return new RestResponse();
  }
}
