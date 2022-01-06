/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.resources.template;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.security.PermissionAttribute.PermissionType.APP_TEMPLATE;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;

import software.wings.beans.CommandCategory;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateVersion;
import software.wings.beans.template.dto.TemplateMetaData;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.security.auth.TemplateAuthHandler;
import software.wings.service.impl.security.auth.TemplateRBACListFilter;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.template.TemplateVersionService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.HashSet;
import java.util.List;
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

@Api("templates")
@Path("/templates")
@Produces("application/json")
@AuthRule(permissionType = LOGGED_IN)
@OwnedBy(PL)
public class TemplateResource {
  @Inject TemplateService templateService;
  @Inject TemplateVersionService templateVersionService;
  @Inject TemplateAuthHandler templateAuthHandler;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Template>> list(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") List<String> appIds,
      @BeanParam PageRequest<Template> pageRequest, @QueryParam("galleryKeys") List<String> galleryKeys,
      @QueryParam("defaultVersion") boolean defaultVersion) {
    final TemplateRBACListFilter templateRBACListFilter = templateAuthHandler.buildTemplateListRBACFilter(appIds);
    if (templateRBACListFilter.empty()) {
      return new RestResponse<>(new PageResponse<>());
    }
    templateRBACListFilter.addToPageRequest(pageRequest);
    return new RestResponse<>(templateService.list(pageRequest, galleryKeys, accountId, defaultVersion));
  }

  /**
   * Save.
   *
   * @param template the template
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = APP_TEMPLATE, skipAuth = true)
  public RestResponse<Template> save(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, Template template) {
    templateAuthHandler.authorizeCreate(appId);
    template.setAccountId(accountId);
    template.setAppId(appId);
    return new RestResponse<>(templateService.save(template));
  }
  /**
   * Updates template.
   *
   * @param template the template
   * @return the rest response
   */
  @PUT
  @Path("{templateId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = APP_TEMPLATE, skipAuth = true)
  public RestResponse<Template> update(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @PathParam("templateId") String templateId,
      Template template) {
    templateAuthHandler.authorizeUpdate(appId, template.getUuid());
    template.setAccountId(accountId);
    template.setAppId(appId);
    template.setUuid(templateId);
    return new RestResponse<>(templateService.update(template));
  }

  /**
   * Delete.
   *
   * @param templateId the template id
   * @return the rest response
   */
  @DELETE
  @Path("{templateId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = APP_TEMPLATE, skipAuth = true)
  public RestResponse delete(@QueryParam("accountId") String accountId, @PathParam("templateId") String templateId) {
    templateAuthHandler.authorizeDelete(accountId, templateId);
    templateService.delete(accountId, templateId);
    return new RestResponse();
  }

  /**
   * Get  template
   * @param accountId
   * @param templateId
   * @return
   */
  @GET
  @Path("{templateId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = APP_TEMPLATE, skipAuth = true)
  public RestResponse<Template> getTemplate(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @PathParam("templateId") String templateId,
      @QueryParam("version") String version) {
    final Template template = templateService.get(accountId, templateId, version);
    templateAuthHandler.authorizeRead(template.getAppId(), templateId);
    return new RestResponse<>(template);
  }

  /***
   * Gets Command Categories of TemplateId
   * @param accountId
   * @return
   */
  @GET
  @Path("{templateId}/commands/categories")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = APP_TEMPLATE, skipAuth = true)
  public RestResponse<List<CommandCategory>> getCommandCategories(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @PathParam("templateId") String templateId) {
    // TODO check is this needed
    final Template template = templateService.get(templateId);
    templateAuthHandler.authorizeRead(template.getAppId(), templateId);
    return new RestResponse<>(templateService.getCommandCategories(accountId, appId, templateId));
  }

  /**
   * Gets the config as code directory by accountId
   *
   * @param accountId the account id
   * @return the rest response
   */
  @GET
  @Path("/tree/search")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = APP_TEMPLATE, skipAuth = true)
  public RestResponse<TemplateFolder> getTemplateTree(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @QueryParam("folderId") String folderId,
      @QueryParam("keyword") String keyword, @QueryParam("type") List<String> templateTypes) {
    return new RestResponse<>(templateService.getTemplateTree(accountId, appId, keyword, templateTypes));
  }

  @GET
  @Path("versions")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<TemplateVersion>> list(@BeanParam PageRequest<TemplateVersion> pageRequest) {
    return new RestResponse<>(templateVersionService.listTemplateVersions(pageRequest));
  }

  @GET
  @Path("{templateId}/versions/{version}/yaml")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = APP_TEMPLATE, skipAuth = true)
  public RestResponse<String> getYaml(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @PathParam("templateId") String templateId,
      @PathParam("version") String version) {
    // TODO check is this needed
    final Template template = templateService.get(accountId, templateId, version);
    templateAuthHandler.authorizeRead(template.getAppId(), templateId);
    return new RestResponse<>(templateService.getYamlOfTemplate(templateId, version));
  }

  @POST
  @Path("/metadata")
  public RestResponse<List<TemplateMetaData>> getTemplateMetadata(
      @QueryParam("accountId") String accountId, List<String> appIds) {
    if (isNotEmpty(appIds)) {
      return new RestResponse<>(templateService.listTemplatesWithMetadata(new HashSet<>(appIds), accountId));
    }
    return new RestResponse();
  }
}
