package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.app.MainConfiguration;
import software.wings.beans.GcpConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static org.eclipse.jetty.util.LazyList.isEmpty;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.GcpConfig.GcpConfigBuilder.aGcpConfig;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;

/**
 * Created by anubhaw on 5/17/16.
 */
@Api("settings")
@Path("/settings")
@Timed
@ExceptionMetered
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class SettingResource {
  @Inject private SettingsService attributeService;
  @Inject private MainConfiguration configuration;

  /**
   * List.
   *
   * @param appId                the app id
   * @param settingVariableTypes the setting variable types
   * @param pageRequest          the page request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<SettingAttribute>> list(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, @QueryParam("type") List<SettingVariableTypes> settingVariableTypes,
      @BeanParam PageRequest<SettingAttribute> pageRequest) {
    if (isNullOrEmpty(appId)) {
      appId = GLOBAL_APP_ID;
    }
    if (!isEmpty(settingVariableTypes)) {
      pageRequest.addFilter(aSearchFilter().withField("value.type", IN, settingVariableTypes.toArray()).build());
    }
    pageRequest.addFilter("appId", appId, EQ);
    return new RestResponse<>(attributeService.list(pageRequest));
  }

  /**
   * Save.
   *
   * @param appId    the app id
   * @param variable the variable
   * @return the rest response
   */
  @POST
  public RestResponse<SettingAttribute> save(@QueryParam("appId") String appId, SettingAttribute variable) {
    if (isNullOrEmpty(appId)) {
      appId = GLOBAL_APP_ID;
    }
    variable.setAppId(appId);
    return new RestResponse<>(attributeService.save(variable));
  }

  /**
   * Save uploaded GCP service account key file.
   *
   * @param uploadedInputStream the uploaded input stream
   * @param fileDetail          the file detail
   * @return the rest response
   */
  @POST
  @Path("upload")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<SettingAttribute> saveUpload(@FormDataParam("appId") String appId,
      @FormDataParam("accountId") String accountId, @FormDataParam("type") String type,
      @FormDataParam("name") String name, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {
    if (isNullOrEmpty(appId)) {
      appId = GLOBAL_APP_ID;
    }
    SettingValue value = null;
    if (GCP.name().equals(type)) {
      value = aGcpConfig().withServiceAccountKeyFileContent(IOUtils.toString(uploadedInputStream)).build();
    }
    if (isNullOrEmpty(name)) {
      String fileName = fileDetail.getFileName();
      name = fileName.substring(0,
          fileName.contains("-") ? fileName.lastIndexOf("-")
                                 : fileName.contains(".") ? fileName.lastIndexOf(".") : fileName.length());
    }
    return new RestResponse<>(attributeService.save(
        aSettingAttribute().withAccountId(accountId).withAppId(appId).withName(name).withValue(value).build()));
  }

  /**
   * Gets the.
   *
   * @param appId  the app id
   * @param attrId the attr id
   * @return the rest response
   */
  @GET
  @Path("{attrId}")
  public RestResponse<SettingAttribute> get(@QueryParam("appId") String appId, @PathParam("attrId") String attrId) {
    if (isNullOrEmpty(appId)) {
      appId = GLOBAL_APP_ID;
    }
    return new RestResponse<>(attributeService.get(appId, attrId));
  }

  /**
   * Update.
   *
   * @param appId    the app id
   * @param attrId   the attr id
   * @param variable the variable
   * @return the rest response
   */
  @PUT
  @Path("{attrId}")
  public RestResponse<SettingAttribute> update(
      @QueryParam("appId") String appId, @PathParam("attrId") String attrId, SettingAttribute variable) {
    if (isNullOrEmpty(appId)) {
      appId = GLOBAL_APP_ID;
    }
    variable.setUuid(attrId);
    variable.setAppId(appId);
    return new RestResponse<>(attributeService.update(variable));
  }

  /**
   * Update.
   *
   * @param appId    the app id
   * @param attrId   the attr id
   * @param variable the variable
   * @param uploadedInputStream the uploaded input stream
   * @param fileDetail          the file detail
   * @return the rest response
   */
  @PUT
  @Path("{attrId}/upload")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<SettingAttribute> update(@QueryParam("appId") String appId, @PathParam("attrId") String attrId,
      SettingAttribute variable, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {
    if (isNullOrEmpty(appId)) {
      appId = GLOBAL_APP_ID;
    }
    variable.setUuid(attrId);
    variable.setAppId(appId);
    if (variable.getValue().getType().equals(GCP.name())) {
      ((GcpConfig) variable.getValue()).setServiceAccountKeyFileContent(IOUtils.toString(uploadedInputStream));
    }
    return new RestResponse<>(attributeService.update(variable));
  }

  /**
   * Delete.
   *
   * @param appId  the app id
   * @param attrId the attr id
   * @return the rest response
   */
  @DELETE
  @Path("{attrId}")
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("attrId") String attrId) {
    if (isNullOrEmpty(appId)) {
      appId = GLOBAL_APP_ID;
    }
    attributeService.delete(appId, attrId);
    return new RestResponse();
  }
}
