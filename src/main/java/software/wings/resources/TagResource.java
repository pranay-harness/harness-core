package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.RestResponse;
import software.wings.beans.Tag;
import software.wings.beans.TagType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.TagService;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Created by anubhaw on 4/25/16.
 */

@Path("/tags")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class TagResource {
  @Inject private TagService tagService;

  @POST
  public RestResponse<Tag> saveTag(Tag tag) {
    return new RestResponse<>(tagService.createTag(tag));
  }

  @POST
  @Path("/types")
  public RestResponse<TagType> saveTagType(TagType tagType) {
    return new RestResponse<>(tagService.createTagType(tagType));
  }

  @POST
  @Path("{tagId}/link/{childTag}")
  public RestResponse<Tag> linkTags(@PathParam("tagId") String tagId, @PathParam("childTag") String childTagId) {
    return new RestResponse<>(tagService.linkTags(tagId, childTagId));
  }

  @GET
  @Path("env/{envId}")
  public RestResponse<List<Tag>> getRootTags(@PathParam("envId") String envId) {
    return new RestResponse<>(tagService.getRootConfigTags(envId));
  }
}
