package software.wings.resources;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static software.wings.security.PermissionAttribute.Action.CREATE;
import static software.wings.security.PermissionAttribute.Action.DELETE;
import static software.wings.security.PermissionAttribute.Action.READ;
import static software.wings.security.PermissionAttribute.Action.UPDATE;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.PermissionType.PROVISIONER;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.api.DeploymentType;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisionerDetails;
import software.wings.beans.NameValuePair;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.ListAPI;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;
import software.wings.service.intfc.InfrastructureProvisionerService;

import java.util.List;
import java.util.Map;
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

@Api("infrastructure-provisioners")
@Path("infrastructure-provisioners")
@Produces("application/json")
@Consumes("application/json")
@Scope(ResourceType.APPLICATION)
public class InfrastructureProvisionerResource {
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;

  @GET
  @Path("blueprint-properties")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<Map<Pair<DeploymentType, CloudProviderType>, Map<String, String>>> blueprintProperties() {
    return new RestResponse<>(InfrastructureMappingBlueprint.infrastructureMappingPropertiesMap);
  }

  @GET
  @Timed
  @ExceptionMetered
  @ListAPI(ResourceType.PROVISIONER)
  @AuthRule(permissionType = PROVISIONER, action = READ)
  public RestResponse<PageResponse<InfrastructureProvisioner>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<InfrastructureProvisioner> pageRequest) {
    return new RestResponse<>(infrastructureProvisionerService.list(pageRequest));
  }

  @GET
  @Path("for-task")
  @Timed
  @ExceptionMetered
  @ListAPI(ResourceType.PROVISIONER)
  @AuthRule(permissionType = PROVISIONER, action = READ)
  public RestResponse<PageResponse<InfrastructureProvisioner>> listForTask(@QueryParam("appId") String appId,
      @QueryParam("type") String infrastructureProvisionerType, @QueryParam("serviceId") String serviceId,
      @QueryParam("deploymentType") DeploymentType deploymentType,
      @QueryParam("cloudProviderType") CloudProviderType cloudProviderType) {
    return new RestResponse<>(infrastructureProvisionerService.listByBlueprintDetails(
        appId, infrastructureProvisionerType, serviceId, deploymentType, cloudProviderType));
  }

  @GET
  @Path("details")
  @Timed
  @ExceptionMetered
  @ListAPI(ResourceType.PROVISIONER)
  @AuthRule(permissionType = PROVISIONER, action = READ)
  public RestResponse<PageResponse<InfrastructureProvisionerDetails>> listDetails(
      @QueryParam("appId") String appId, @BeanParam PageRequest<InfrastructureProvisioner> pageRequest) {
    if (appId != null) {
      pageRequest.addFilter("appId", EQ, appId);
    }
    return new RestResponse<>(infrastructureProvisionerService.listDetails(pageRequest));
  }

  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PROVISIONER, action = CREATE)
  public RestResponse<InfrastructureProvisioner> save(
      @QueryParam("appId") String appId, InfrastructureProvisioner infrastructureProvisioner) {
    infrastructureProvisioner.setAppId(appId);
    return new RestResponse<>(infrastructureProvisionerService.save(infrastructureProvisioner));
  }

  @PUT
  @Path("{infraProvisionerId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PROVISIONER, action = UPDATE)
  public RestResponse<InfrastructureProvisioner> update(@QueryParam("appId") String appId,
      @PathParam("infraProvisionerId") String infraProvisionerId, InfrastructureProvisioner infrastructureProvisioner) {
    infrastructureProvisioner.setUuid(infraProvisionerId);
    infrastructureProvisioner.setAppId(appId);
    return new RestResponse<>(infrastructureProvisionerService.update(infrastructureProvisioner));
  }

  @GET
  @Path("{infraProvisionerId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PROVISIONER, action = READ)
  public RestResponse<InfrastructureProvisioner> get(
      @QueryParam("appId") String appId, @PathParam("infraProvisionerId") String infraProvisionerId) {
    return new RestResponse<>(infrastructureProvisionerService.get(appId, infraProvisionerId));
  }

  @POST
  @Path("get-params")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AwsCFTemplateParamsData>> getParamsKeys(@QueryParam("type") String type,
      @QueryParam("region") String region, @QueryParam("awsConfigId") String awsConfigId,
      @QueryParam("appId") String appId, String data) {
    return new RestResponse<>(
        infrastructureProvisionerService.getCFTemplateParamKeys(type, region, awsConfigId, data, appId));
  }

  @DELETE
  @Path("{infraProvisionerId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PROVISIONER, action = DELETE)
  public RestResponse delete(
      @QueryParam("appId") String appId, @PathParam("infraProvisionerId") String infraProvisionerId) {
    infrastructureProvisionerService.delete(appId, infraProvisionerId);
    return new RestResponse();
  }

  @GET
  @Path("terraform-variables")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PROVISIONER, action = READ)
  public RestResponse<List<NameValuePair>> getTerraformVariables(@QueryParam("appId") String appId,
      @QueryParam("sourceRepoSettingId") String scmSettingId, @QueryParam("path") String terraformDirectory,
      @QueryParam("accountId") String accountId) {
    return new RestResponse<>(
        infrastructureProvisionerService.getTerraformVariables(appId, scmSettingId, terraformDirectory, accountId));
  }
}
