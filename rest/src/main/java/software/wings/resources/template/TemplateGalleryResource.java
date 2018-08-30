package software.wings.resources.template;

import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("template-galleries")
@Path("/template-galleries")
@Produces("application/json")
@Scope(PermissionAttribute.ResourceType.TEMPLATE)
@AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
public class TemplateGalleryResource {
  @Inject TemplateGalleryService templateGalleryService;
  @Inject TemplateFolderService templateFolderService;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<TemplateGallery>> list(
      @QueryParam("accountId") String accountId, @BeanParam PageRequest<TemplateGallery> request) {
    return new RestResponse<>(templateGalleryService.list(request));
  }

  /**
   * Save.
   *
   * @param accountId   the account id
   * @param templateGallery the template gallery
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<TemplateGallery> save(
      @QueryParam("accountId") String accountId, TemplateGallery templateGallery) {
    templateGallery.setAccountId(accountId);
    return new RestResponse<>(templateGalleryService.save(templateGallery));
  }

  /**
   * Updates template.
   * @param templateGallery the template gallery
   * @return the rest response
   */
  @PUT
  @Path("{templateGalleryId}")
  @Timed
  @ExceptionMetered
  public RestResponse<TemplateGallery> update(@QueryParam("accountId") String accountId,
      @PathParam("templateGalleryId") String templateId, TemplateGallery templateGallery) {
    return new RestResponse<>(templateGalleryService.update(templateGallery));
  }

  /**
   * Delete.
   *
   * @param galleryId the gallery id
   * @return the rest response
   */
  @DELETE
  @Path("{galleryId}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@PathParam("galleryId") String galleryId) {
    templateGalleryService.delete(galleryId);
    return new RestResponse();
  }

  /**
   * Get  template
   * @param accountId
   * @param galleryId
   * @return
   */
  @GET
  @Path("{galleryId}")
  @Timed
  @ExceptionMetered
  public RestResponse<TemplateGallery> getTemplateGallery(
      @QueryParam("accountId") String accountId, @PathParam("galleryId") String galleryId) {
    return new RestResponse<>(templateGalleryService.get(galleryId));
  }

  @POST
  @Path("folders")
  @Timed
  @ExceptionMetered
  public RestResponse<TemplateFolder> saveFolder(
      @QueryParam("accountId") String accountId, TemplateFolder templateFolder) {
    templateFolder.setAppId(GLOBAL_APP_ID);
    templateFolder.setAccountId(accountId);
    return new RestResponse<>(templateFolderService.save(templateFolder));
  }

  /**
   * Updates template.
   * @param templateFolder the Template folder
   * @return the rest response
   */
  @PUT
  @Path("folders/{templateFolderId}")
  @Timed
  @ExceptionMetered
  public RestResponse<TemplateFolder> updateFolder(@QueryParam("accountId") String accountId,
      @PathParam("templateFolderId") String templateFolderId, TemplateFolder templateFolder) {
    templateFolder.setAppId(GLOBAL_APP_ID);
    templateFolder.setUuid(templateFolderId);
    return new RestResponse<>(templateFolderService.update(templateFolder));
  }

  /***
   * Get template folder
   * @param accountId
   * @param templateFolderId
   * @return
   */
  @GET
  @Path("folders/{templateFolderId}")
  @Timed
  @ExceptionMetered
  public RestResponse<TemplateFolder> getTemplateFolder(
      @QueryParam("accountId") String accountId, @PathParam("templateFolderId") String templateFolderId) {
    return new RestResponse<>(templateFolderService.get(templateFolderId));
  }

  /**
   * Delete.
   *
   * @param templateFolderId the gallery id
   * @return the rest response
   */
  @DELETE
  @Path("folders/{templateFolderId}")
  @Timed
  @ExceptionMetered
  public RestResponse deleteFolder(@PathParam("templateFolderId") String templateFolderId) {
    templateFolderService.delete(templateFolderId);
    return new RestResponse();
  }
}
