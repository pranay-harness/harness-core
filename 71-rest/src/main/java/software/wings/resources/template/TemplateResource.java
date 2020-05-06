package software.wings.resources.template;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.beans.CommandCategory;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateVersion;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.template.TemplateVersionService;

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
public class TemplateResource {
  @Inject TemplateService templateService;
  @Inject TemplateVersionService templateVersionService;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Template>> list(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @BeanParam PageRequest<Template> pageRequest,
      @QueryParam("galleryKeys") List<String> galleryKeys) {
    pageRequest.addFilter("appId", EQ, appId);
    return new RestResponse<>(templateService.list(pageRequest, galleryKeys, accountId));
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
  @AuthRule(permissionType = TEMPLATE_MANAGEMENT)
  public RestResponse<Template> save(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, Template template) {
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
  @AuthRule(permissionType = TEMPLATE_MANAGEMENT)
  public RestResponse<Template> update(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @PathParam("templateId") String templateId,
      Template template) {
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
  @AuthRule(permissionType = TEMPLATE_MANAGEMENT)
  public RestResponse delete(@QueryParam("accountId") String accountId, @PathParam("templateId") String templateId) {
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
  public RestResponse<Template> getTemplate(@QueryParam("accountId") String accountId,
      @PathParam("templateId") String templateId, @QueryParam("version") String version) {
    return new RestResponse<>(templateService.get(accountId, templateId, version));
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
  public RestResponse<List<CommandCategory>> getCommandCategories(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @PathParam("templateId") String templateId) {
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
  public RestResponse<String> getYaml(@QueryParam("accountId") String accountId,
      @PathParam("templateId") String templateId, @QueryParam("version") Long version) {
    return new RestResponse<>(templateService.getYamlOfTemplate(templateId, version));
  }
}
