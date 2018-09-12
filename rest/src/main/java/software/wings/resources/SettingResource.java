package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.common.Constants.GIT_USER;
import static software.wings.service.impl.security.SecretManagerImpl.ENCRYPTED_FIELD_MASK;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.swagger.annotations.Api;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.annotation.Encryptable;
import software.wings.beans.GcpConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.ValidationResult;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 5/17/16.
 */
@Api("settings")
@Path("/settings")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Scope(ResourceType.SETTING)
public class SettingResource {
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;

  /**
   * List.
   *
   * @param appId                the app id
   * @param settingVariableTypes the setting variable types
   * @param pageRequest          the page request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<SettingAttribute>> list(
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @QueryParam("currentAppId") String currentAppId,
      @QueryParam("currentEnvId") String currentEnvId, @QueryParam("accountId") String accountId,
      @QueryParam("type") List<SettingVariableTypes> settingVariableTypes,
      @QueryParam("gitSshConfigOnly") boolean gitSshConfigOnly, @BeanParam PageRequest<SettingAttribute> pageRequest) {
    pageRequest.addFilter("appId", EQ, appId);
    if (isNotEmpty(settingVariableTypes)) {
      pageRequest.addFilter("value.type", IN, settingVariableTypes.toArray());
    }

    if (gitSshConfigOnly) {
      pageRequest.addFilter("accountId", EQ, accountId);
      pageRequest.addFilter("value.type", EQ, SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES.name());
    }

    PageResponse<SettingAttribute> result = settingsService.list(pageRequest, currentAppId, currentEnvId);
    if (gitSshConfigOnly) {
      List<SettingAttribute> filteredResponse =
          result.stream()
              .filter(settingAttribute
                  -> GIT_USER.equals(((HostConnectionAttributes) settingAttribute.getValue()).getUserName()))
              .collect(Collectors.toList());
      result.setResponse(filteredResponse);
    }

    result.forEach(this ::maskEncryptedFields);
    return new RestResponse<>(result);
  }

  private void maskEncryptedFields(SettingAttribute settingAttribute) {
    SettingValue settingValue = settingAttribute.getValue();
    if (settingValue instanceof Encryptable) {
      secretManager.maskEncryptedFields((Encryptable) settingValue);
    }
  }

  private void prePruneSettingAttribute(final String appId, final String accountId, final SettingAttribute variable) {
    variable.setAppId(appId);
    if (accountId != null) {
      variable.setAccountId(accountId);
    }
    if (variable.getValue() != null) {
      if (variable.getValue() instanceof Encryptable) {
        ((Encryptable) variable.getValue()).setAccountId(variable.getAccountId());
      }
    }
    variable.setCategory(Category.getCategory(SettingVariableTypes.valueOf(variable.getValue().getType())));
  }

  /**
   * Save.
   *
   * @param appId    the app id
   * @param variable the variable
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<SettingAttribute> save(@DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, SettingAttribute variable) {
    prePruneSettingAttribute(appId, accountId, variable);
    return new RestResponse<>(settingsService.save(variable));
  }

  /**
   * Validate
   * @param appId The appId
   * @param accountId The account Id
   * @param variable The variable to be validated
   * @return
   */
  @POST
  @Path("validate")
  @Timed
  @ExceptionMetered
  public RestResponse<ValidationResult> validate(@DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, SettingAttribute variable) {
    prePruneSettingAttribute(appId, accountId, variable);
    return new RestResponse<>(settingsService.validate(variable));
  }

  /**
   * Save uploaded GCP service account key file.
   *
   * @return the rest response
   */
  @SuppressFBWarnings({"NP_NULL_ON_SOME_PATH", "BC_VACUOUS_INSTANCEOF"}) // TODO
  @POST
  @Path("upload")
  @Consumes(MULTIPART_FORM_DATA)
  @Timed
  @ExceptionMetered
  public RestResponse<SettingAttribute> saveUpload(@DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, @FormDataParam("type") String type, @FormDataParam("name") String name,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {
    if (uploadedInputStream == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Missing file.");
    }

    SettingValue value = null;
    if (GCP.name().equals(type)) {
      value = GcpConfig.builder()
                  .serviceAccountKeyFileContent(
                      IOUtils.toString(uploadedInputStream, Charset.defaultCharset()).toCharArray())
                  .build();
    }
    if (null != value) {
      if (value instanceof Encryptable) {
        ((Encryptable) value).setAccountId(accountId);
      }
    }
    return new RestResponse<>(
        settingsService.save(aSettingAttribute()
                                 .withAccountId(accountId)
                                 .withAppId(appId)
                                 .withName(name)
                                 .withValue(value)
                                 .withCategory(Category.getCategory(SettingVariableTypes.valueOf(value.getType())))
                                 .build()));
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
  @Timed
  @ExceptionMetered
  public RestResponse<SettingAttribute> get(
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @PathParam("attrId") String attrId) {
    SettingAttribute result = settingsService.get(appId, attrId);
    maskEncryptedFields(result);
    return new RestResponse<>(result);
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
  @Timed
  @ExceptionMetered
  public RestResponse<SettingAttribute> update(@DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @PathParam("attrId") String attrId, SettingAttribute variable) {
    variable.setUuid(attrId);
    variable.setAppId(appId);
    if (variable.getValue() != null) {
      if (variable.getValue() instanceof Encryptable) {
        ((Encryptable) variable.getValue()).setAccountId(variable.getAccountId());
      }
    }
    return new RestResponse<>(settingsService.update(variable));
  }

  /**
   * Update.
   *
   * @return the rest response
   */
  @PUT
  @Path("{attrId}/upload")
  @Consumes(MULTIPART_FORM_DATA)
  @Timed
  @ExceptionMetered
  @SuppressFBWarnings("BC_VACUOUS_INSTANCEOF")
  public RestResponse<SettingAttribute> update(@PathParam("attrId") String attrId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @QueryParam("accountId") String accountId,
      @FormDataParam("type") String type, @FormDataParam("name") String name,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {
    char[] credentials = IOUtils.toString(uploadedInputStream, Charset.defaultCharset()).toCharArray();
    SettingValue value = null;
    if (GCP.name().equals(type)) {
      if (credentials != null && credentials.length > 0) {
        value = GcpConfig.builder().serviceAccountKeyFileContent(credentials).build();
      } else {
        value = GcpConfig.builder().serviceAccountKeyFileContent(ENCRYPTED_FIELD_MASK).build();
      }
    }
    SettingAttribute.Builder settingAttribute =
        aSettingAttribute().withUuid(attrId).withName(name).withAccountId(accountId).withAppId(appId).withCategory(
            Category.getCategory(SettingVariableTypes.valueOf(type)));
    if (value != null) {
      if (value instanceof Encryptable) {
        ((Encryptable) value).setAccountId(accountId);
      }
      settingAttribute.withValue(value);
    }
    return new RestResponse<>(settingsService.update(settingAttribute.build()));
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
  @Timed
  @ExceptionMetered
  public RestResponse delete(
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @PathParam("attrId") String attrId) {
    settingsService.delete(appId, attrId);
    return new RestResponse();
  }
}
