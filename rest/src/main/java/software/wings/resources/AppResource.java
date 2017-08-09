package software.wings.resources;

import static software.wings.beans.Setup.SetupStatus.COMPLETE;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;
import software.wings.beans.Setup.SetupStatus;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.ListAPI;
import software.wings.service.intfc.AppService;
import software.wings.yaml.YamlPayload;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
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
import javax.ws.rs.core.Response;

/**
 * Application Resource class.
 *
 * @author Rishi
 */
@Api("/apps")
@Path("/apps")
@Produces("application/json")
@AuthRule(APPLICATION)
public class AppResource {
  private AppService appService;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Instantiates a new app resource.
   *
   * @param appService the app service
   */
  @Inject
  public AppResource(AppService appService) {
    this.appService = appService;
  }

  /**
   * List.
   *
   * @param pageRequest        the page request
   * @param overview           the summary
   * @param numberOfExecutions the number of executions
   * @return the rest response
   */
  @GET
  @ListAPI(APPLICATION)
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Application>> list(@BeanParam PageRequest<Application> pageRequest,
      @QueryParam("overview") @DefaultValue("false") boolean overview,
      @QueryParam("overviewDays") @DefaultValue("30") int overviewDays,
      @QueryParam("numberOfExecutions") @DefaultValue("5") int numberOfExecutions,
      @QueryParam("appIds") List<String> appIds) {
    return new RestResponse<>(appService.list(pageRequest, overview, numberOfExecutions, overviewDays));
  }

  /**
   * Save.
   *
   * @param app the app
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Application> save(@QueryParam("accountId") String accountId, Application app) {
    app.setAccountId(accountId);
    return new RestResponse<>(appService.save(app));
  }

  /**
   * Update.
   *
   * @param appId the app id
   * @param app   the app
   * @return the rest response
   */
  @PUT
  @Path("{appId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Application> update(@PathParam("appId") String appId, Application app) {
    app.setUuid(appId);
    return new RestResponse<>(appService.update(app));
  }

  /**
   * Gets the.
   *
   * @param appId  the app id
   * @param status the status
   * @return the rest response
   */
  @GET
  @Path("{appId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Application> get(@PathParam("appId") String appId, @QueryParam("status") SetupStatus status,
      @QueryParam("overview") @DefaultValue("false") boolean overview,
      @QueryParam("overviewDays") @DefaultValue("30") int overviewDays,
      @QueryParam("yaml") @DefaultValue("false") boolean yaml) {
    if (status == null) {
      status = COMPLETE; // don't verify setup status
    }

    logger.info("****************** yaml = " + yaml);

    return new RestResponse<>(appService.get(appId, status, true, overviewDays));
  }

  /**
   * Gets the yaml version of an app by addId
   *
   * @param appId  the app id
   * @param status the status
   * @return the response
   */
  @GET
  @Path("/yaml/{appId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getYaml(@PathParam("appId") String appId, @QueryParam("status") SetupStatus status,
      @QueryParam("overview") @DefaultValue("false") boolean overview,
      @QueryParam("overviewDays") @DefaultValue("30") int overviewDays) {
    if (status == null) {
      status = COMPLETE; // don't verify setup status
    }

    Application app = appService.get(appId, status, true, overviewDays);

    RestResponse rr = new RestResponse<>();

    if (app != null) {
      YamlPayload yp = new YamlPayload(app.getYaml());
      rr.setResponseMessages(yp.getResponseMessages());

      if (yp.getYaml() != null && !yp.getYaml().isEmpty()) {
        rr.setResource(yp);
      }
    }

    return rr;
  }

  /**
   * Save an app that is sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId  the account id
   * @param yamlPayload the yaml version of app
   * @return the rest response
   */
  @POST
  @Path("/yaml")
  @Timed
  @ExceptionMetered
  public RestResponse<Application> saveFromYaml(@QueryParam("accountId") String accountId, YamlPayload yamlPayload) {
    String yaml = yamlPayload.getYaml();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Application app = null;

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    if (yaml != null && !yaml.isEmpty()) {
      try {
        app = mapper.readValue(yaml, Application.class);
        app.setAccountId(accountId);

        app = appService.save(app);

        if (app != null) {
          rr.setResource(app);
        }
      } catch (Exception e) {
        addUnrecognizedFieldsMessage(rr);
      }
    }

    return rr;
  }

  /**
   * Update an app that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId  the app id
   * @param yamlPayload the yaml version of app
   * @return the rest response
   */
  @PUT
  @Path("/yaml/{appId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Application> updateFromYaml(@PathParam("appId") String appId, YamlPayload yamlPayload) {
    String yaml = yamlPayload.getYaml();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    Application app = null;

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    if (yaml != null && !yaml.isEmpty()) {
      try {
        app = mapper.readValue(yaml, Application.class);
        app.setUuid(appId);

        app = appService.update(app);

        if (app != null) {
          rr.setResource(app);
        }
      } catch (Exception e) {
        addUnrecognizedFieldsMessage(rr);
      }
    }

    return rr;
  }

  /**
   * Delete.
   *
   * @param appId the app id
   * @return the rest response
   */
  @DELETE
  @Path("{appId}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@PathParam("appId") String appId) {
    appService.delete(appId);
    return new RestResponse();
  }

  private void addUnrecognizedFieldsMessage(RestResponse rr) {
    ResponseMessage rm = new ResponseMessage();
    rm.setCode(ErrorCode.UNRECOGNIZED_YAML_FIELDS);
    rm.setErrorType(ResponseTypeEnum.ERROR);
    rm.setMessage("ERROR: The Yaml provided contains unrecognized fields!");

    List<ResponseMessage> responseMessages = rr.getResponseMessages();
    responseMessages.add(rm);
    rr.setResponseMessages(responseMessages);
  }
}
