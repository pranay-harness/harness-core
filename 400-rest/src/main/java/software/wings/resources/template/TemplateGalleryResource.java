/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.template;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;

import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.security.auth.TemplateAuthHandler;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
@Scope(ResourceType.TEMPLATE)
@AuthRule(permissionType = LOGGED_IN)
@OwnedBy(HarnessTeam.PL)
public class TemplateGalleryResource {
  @Inject TemplateGalleryService templateGalleryService;
  @Inject TemplateFolderService templateFolderService;
  @Inject TemplateAuthHandler templateAuthHandler;

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
  @AuthRule(permissionType = TEMPLATE_MANAGEMENT)
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
  @AuthRule(permissionType = TEMPLATE_MANAGEMENT)
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
  @AuthRule(permissionType = TEMPLATE_MANAGEMENT)
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
  @AuthRule(permissionType = TEMPLATE_MANAGEMENT)
  public RestResponse<TemplateFolder> saveFolder(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, TemplateFolder templateFolder) {
    templateAuthHandler.authorizeTemplateFolderCrud(appId, Action.CREATE);
    templateFolder.setAppId(appId);
    templateFolder.setAccountId(accountId);
    if (EmptyPredicate.isEmpty(templateFolder.getParentId())) {
      throw new WingsException("Root folder can not be added. Only one root folder supported ", WingsException.USER);
    }
    // TODO: UI needs to send gallery ID while saving folder. For now making account gallery as default if not set.
    String galleryId = templateFolder.getGalleryId();
    if (galleryId == null) {
      TemplateGallery templateGallery =
          templateGalleryService.getByAccount(accountId, templateGalleryService.getAccountGalleryKey());
      galleryId = templateGallery.getUuid();
    }
    return new RestResponse<>(templateFolderService.save(templateFolder, galleryId));
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
  @AuthRule(permissionType = TEMPLATE_MANAGEMENT)
  public RestResponse<TemplateFolder> updateFolder(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @PathParam("templateFolderId") String templateFolderId, TemplateFolder templateFolder) {
    templateAuthHandler.authorizeTemplateFolderCrud(appId, Action.UPDATE);
    templateFolder.setAppId(appId);
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
  @AuthRule(permissionType = TEMPLATE_MANAGEMENT)
  public RestResponse deleteFolder(@DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @PathParam("templateFolderId") String templateFolderId) {
    templateAuthHandler.authorizeTemplateFolderCrud(appId, Action.DELETE);
    templateFolderService.delete(templateFolderId);
    return new RestResponse();
  }
}
