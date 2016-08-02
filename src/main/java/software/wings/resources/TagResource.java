package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.Tag;
import software.wings.beans.Tag.TagType;
import software.wings.beans.UuidList;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.TagService;

import java.util.List;
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
   * @param flatten the flatten
   * @param request the request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<Tag>> list(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @QueryParam("flatten") boolean flatten, @BeanParam PageRequest<Tag> request) {
    request.addFilter("appId", appId, EQ);
    request.addFilter("envId", envId, EQ);
    request.addFilter("tagType", TagType.ENVIRONMENT, EQ);
    return new RestResponse<>(tagService.list(request));
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
    return new RestResponse<>(tagService.save(parentTagId, tag));
  }

  /**
   * Gets the.
   *
   * @param appId the app id
   * @param envId the env id
   * @param tagId the tag id
   * @return the rest response
   */
  @GET
  @Path("{tagId}")
  public RestResponse<Tag> get(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @PathParam("tagId") String tagId) {
    return new RestResponse<>(tagService.get(appId, envId, tagId));
  }

  /**
   * Update.
   *
   * @param appId the app id
   * @param envId the env id
   * @param tagId the tag id
   * @param tag   the tag
   * @return the rest response
   */
  @PUT
  @Path("{tagId}")
  public RestResponse<Tag> update(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @PathParam("tagId") String tagId, Tag tag) {
    tag.setAppId(appId);
    tag.setEnvId(envId);
    tag.setUuid(tagId);
    return new RestResponse<>(tagService.update(tag));
  }

  /**
   * Delete.
   *
   * @param appId the app id
   * @param envId the env id
   * @param tagId the tag id
   * @return the rest response
   */
  @DELETE
  @Path("{tagId}")
  public RestResponse delete(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @PathParam("tagId") String tagId) {
    tagService.delete(appId, envId, tagId);
    return new RestResponse();
  }

  /**
   * Tag hosts.
   *
   * @param appId    the app id
   * @param envId    the env id
   * @param tagId    the tag id
   * @param uuidList the uuid list
   * @return the rest response
   */
  @POST
  @Path("{tagId}/tag-hosts")
  public RestResponse tagHosts(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("tagId") String tagId, UuidList uuidList) {
    tagService.tagHostsByApi(appId, envId, tagId, uuidList.getUuids());
    return new RestResponse();
  }

  /**
   * Tag tree rest response.
   *
   * @param appId the app id
   * @param envId the env id
   * @param tagId the tag id
   * @return the rest response
   */
  @GET
  @Path("/flatten-tree")
  public RestResponse<List<Tag>> tagTree(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @QueryParam("tagId") String tagId) {
    return new RestResponse<>(tagService.flattenTagTree(appId, envId, tagId));
  }

  @GET
  @Path("/leaf-tags")
  public RestResponse<List<Tag>> leafTags(@QueryParam("appId") String appId, @QueryParam("envId") String envId) {
    return new RestResponse<>(tagService.getUserCreatedLeafTags(appId, envId));
  }
}
