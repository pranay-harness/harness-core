package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.Tag;
import software.wings.beans.TagType;
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

@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class TagResource {
  @Inject private TagService tagService;

  @GET
  @Path("tag-types")
  public RestResponse<PageResponse<TagType>> listTagType(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @BeanParam PageRequest<TagType> request) {
    request.addFilter("appId", appId, EQ);
    request.addFilter("envId", envId, EQ);
    return new RestResponse<>(tagService.listTagType(request));
  }

  @POST
  @Path("tag-types")
  public RestResponse<TagType> saveTagType(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, TagType tagType) {
    tagType.setEnvId(envId);
    tagType.setAppId(appId);
    return new RestResponse<>(tagService.saveTagType(tagType));
  }

  @GET
  @Path("tag-types/{tagTypeId}")
  public RestResponse<TagType> getTagType(@QueryParam("appId") String appId, @PathParam("tagTypeId") String tagTypeId) {
    return new RestResponse<>(tagService.getTagType(appId, tagTypeId));
  }

  @PUT
  @Path("tag-types/{tagTypeId}")
  public RestResponse<TagType> updateTagType(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("tagTypeId") String tagTypeId, TagType tagType) {
    tagType.setUuid(tagTypeId);
    tagType.setEnvId(envId);
    tagType.setAppId(appId);
    return new RestResponse<>(tagService.updateTagType(tagType));
  }

  @DELETE
  @Path("tag-types/{tagTypeId}")
  public RestResponse deleteTagType(@QueryParam("appId") String appId, @PathParam("tagTypeId") String tagTypeId) {
    tagService.deleteTagType(appId, tagTypeId);
    return new RestResponse();
  }

  @GET
  @Path("tags")
  public RestResponse<PageResponse<Tag>> listTag(@QueryParam("appId") String appId,
      @QueryParam("tagTypeId") String tagTypeId, @BeanParam PageRequest<Tag> request) {
    request.addFilter("appId", appId, EQ);
    return new RestResponse<>(tagService.listTag(tagTypeId, request));
  }

  @POST
  @Path("tags")
  public RestResponse<Tag> saveTag(
      @QueryParam("appId") String appId, @QueryParam("tagTypeId") String tagTypeId, Tag tag) {
    tag.setAppId(appId);
    tag.getTagType().setUuid(tagTypeId);
    return new RestResponse<>(tagService.saveTag(tag));
  }

  @GET
  @Path("tags/{tagId}")
  public RestResponse<Tag> getTag(@QueryParam("appId") String appId, @PathParam("tagId") String tagId) {
    return new RestResponse<>(tagService.getTag(appId, tagId));
  }

  @PUT
  @Path("tags/{tagId}")
  public RestResponse<Tag> updateTag(@QueryParam("appId") String appId, @QueryParam("tagTypeId") String tagTypeId,
      @PathParam("tagId") String tagId, Tag tag) {
    tag.setAppId(appId);
    tag.setUuid(tagId);
    tag.getTagType().setUuid(tagTypeId);
    return new RestResponse<>(tagService.updateTag(tag));
  }

  @DELETE
  @Path("tags/{tagId}")
  public RestResponse deleteTag(@QueryParam("appId") String appId, @PathParam("tagId") String tagId) {
    tagService.deleteTag(appId, tagId);
    return new RestResponse();
  }

  @POST
  @Path("{tagId}/link/{childTag}")
  public RestResponse<Tag> linkTags(
      @QueryParam("appId") String appId, @PathParam("tagId") String tagId, @PathParam("childTag") String childTagId) {
    return new RestResponse<>(tagService.linkTags(appId, tagId, childTagId));
  }

  @GET
  @Path("env/{envId}")
  public RestResponse<List<Tag>> getRootTags(@PathParam("envId") String envId) {
    return new RestResponse<>(tagService.getRootConfigTags(envId));
  }
}
