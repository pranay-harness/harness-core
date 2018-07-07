package software.wings.resources.template;

import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.CommandCategory;
import software.wings.beans.RestResponse;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateVersion;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.template.TemplateVersionService;

import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
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
@Scope(PermissionAttribute.ResourceType.TEMPLATE)
@AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
public class TemplateResource {
  @Inject TemplateService templateService;
  @Inject TemplateVersionService templateVersionService;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Template>> list(
      @QueryParam("accountId") String accountId, @BeanParam PageRequest<Template> pageRequest) {
    return new RestResponse<>(templateService.list(pageRequest));
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
  public RestResponse<Template> save(@QueryParam("accountId") String accountId, Template template) {
    template.setAccountId(accountId);
    template.setAppId(GLOBAL_APP_ID);
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
  public RestResponse<Template> update(
      @QueryParam("accountId") String accountId, @PathParam("templateId") String templateId, Template template) {
    template.setAccountId(accountId);
    template.setAppId(GLOBAL_APP_ID);
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
  public RestResponse<List<CommandCategory>> getCommandCategories(
      @QueryParam("accountId") String accountId, @PathParam("templateId") String templateId) {
    return new RestResponse<>(templateService.getCommandCategories(accountId, templateId));
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
      @QueryParam("folderId") String folderId, @QueryParam("keyword") String keyword,
      @QueryParam("type") List<String> templateTypes) {
    return new RestResponse<>(templateService.getTemplateTree(accountId, keyword, templateTypes));
  }

  @GET
  @Path("versions")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<TemplateVersion>> list(@BeanParam PageRequest<TemplateVersion> pageRequest) {
    return new RestResponse<>(templateVersionService.listTemplateVersions(pageRequest));
  }
}
