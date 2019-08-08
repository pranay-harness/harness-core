package software.wings.resources;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.service.impl.security.SecretManagerImpl.ENCRYPTED_FIELD_MASK;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.CustomArtifactServerConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.NameValuePair;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.ValidationResult;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.common.BuildDetailsComparator;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.JobDetails;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.AwsHelperResourceService;
import software.wings.service.intfc.AzureResourceService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.utils.RepositoryFormat;
import software.wings.utils.RepositoryType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
@Slf4j
@Api("settings")
@Path("/settings")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Scope(ResourceType.SETTING)
public class SettingResource {
  private static final String LIMIT = "" + Integer.MAX_VALUE;
  @Inject private SettingsService settingsService;
  @Inject private BuildSourceService buildSourceService;
  @Inject private UsageRestrictionsService usageRestrictionsService;
  @Inject private SecretManager secretManager;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactService artifactService;
  @Inject private AzureResourceService azureResourceService;
  @Inject private AwsHelperResourceService awsHelperResourceService;
  @Inject private YamlGitService yamlGitService;
  @Inject private AccountService accountService;

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
      @QueryParam("gitSshConfigOnly") boolean gitSshConfigOnly,
      @QueryParam("withArtifactStreamCount") boolean withArtifactStreamCount,
      @QueryParam("artifactStreamSearchString") String artifactStreamSearchString,
      @DefaultValue(LIMIT) @QueryParam("maxResults") int maxResults, @QueryParam("serviceId") String serviceId,
      @BeanParam PageRequest<SettingAttribute> pageRequest) {
    pageRequest.addFilter("appId", EQ, appId);
    if (isNotEmpty(settingVariableTypes)) {
      pageRequest.addFilter("value.type", IN, settingVariableTypes.toArray());
    }

    if (gitSshConfigOnly) {
      pageRequest.addFilter("accountId", EQ, accountId);
      pageRequest.addFilter("value.type", EQ, SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES.name());
    }

    PageResponse<SettingAttribute> result = settingsService.list(pageRequest, currentAppId, currentEnvId, accountId,
        gitSshConfigOnly, withArtifactStreamCount, artifactStreamSearchString, maxResults, serviceId);
    result.forEach(this ::maskEncryptedFields);
    return new RestResponse<>(result);
  }

  private void maskEncryptedFields(SettingAttribute settingAttribute) {
    SettingValue settingValue = settingAttribute.getValue();
    if (settingValue instanceof EncryptableSetting) {
      secretManager.maskEncryptedFields((EncryptableSetting) settingValue);
    }
  }

  private void prePruneSettingAttribute(final String appId, final String accountId, final SettingAttribute variable) {
    variable.setAppId(appId);
    if (accountId != null) {
      variable.setAccountId(accountId);
    }
    if (variable.getValue() != null) {
      if (variable.getValue() instanceof EncryptableSetting) {
        ((EncryptableSetting) variable.getValue()).setAccountId(variable.getAccountId());
        ((EncryptableSetting) variable.getValue()).setDecrypted(true);
      }
      if (variable.getValue() instanceof CustomArtifactServerConfig) {
        ((CustomArtifactServerConfig) variable.getValue()).setAccountId(variable.getAccountId());
      }
    }
    variable.setCategory(SettingCategory.getCategory(SettingVariableTypes.valueOf(variable.getValue().getType())));
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

  @POST
  @Path("validate-connectivity")
  @Timed
  @ExceptionMetered
  public RestResponse<ValidationResult> validateConnectivity(
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @QueryParam("accountId") String accountId,
      SettingAttribute variable) {
    prePruneSettingAttribute(appId, accountId, variable);
    return new RestResponse<>(settingsService.validateConnectivity(variable));
  }

  /**
   * Save uploaded GCP service account key file.
   *
   * @return the rest response
   */
  @POST
  @Path("upload")
  @Consumes(MULTIPART_FORM_DATA)
  @Timed
  @ExceptionMetered
  public RestResponse<SettingAttribute> saveUpload(@DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, @FormDataParam("type") String type, @FormDataParam("name") String name,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @FormDataParam("usageRestrictions") final String usageRestrictionsString) throws IOException {
    if (uploadedInputStream == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Missing file.");
    }

    if (GCP.name().equals(type)) {
      SettingValue value = GcpConfig.builder()
                               .serviceAccountKeyFileContent(
                                   IOUtils.toString(uploadedInputStream, Charset.defaultCharset()).toCharArray())
                               .build();

      ((EncryptableSetting) value).setAccountId(accountId);
      ((EncryptableSetting) value).setDecrypted(true);

      UsageRestrictions usageRestrictionsFromJson =
          usageRestrictionsService.getUsageRestrictionsFromJson(usageRestrictionsString);

      return new RestResponse<>(settingsService.save(
          aSettingAttribute()
              .withAccountId(accountId)
              .withAppId(appId)
              .withName(name)
              .withValue(value)
              .withCategory(SettingCategory.getCategory(SettingVariableTypes.valueOf(value.getType())))
              .withUsageRestrictions(usageRestrictionsFromJson)
              .build()));
    }
    return new RestResponse<>();
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
      if (variable.getValue() instanceof EncryptableSetting) {
        ((EncryptableSetting) variable.getValue()).setAccountId(variable.getAccountId());
        ((EncryptableSetting) variable.getValue()).setDecrypted(true);
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
  public RestResponse<SettingAttribute> update(@PathParam("attrId") String attrId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @QueryParam("accountId") String accountId,
      @FormDataParam("type") String type, @FormDataParam("name") String name,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @FormDataParam("usageRestrictions") final String usageRestrictionsString) throws IOException {
    char[] credentials = IOUtils.toString(uploadedInputStream, Charset.defaultCharset()).toCharArray();
    SettingValue value = null;
    if (GCP.name().equals(type)) {
      if (credentials.length > 0) {
        value = GcpConfig.builder().serviceAccountKeyFileContent(credentials).build();
      } else {
        value = GcpConfig.builder().serviceAccountKeyFileContent(ENCRYPTED_FIELD_MASK.toCharArray()).build();
      }
    }

    UsageRestrictions usageRestrictionsFromJson =
        usageRestrictionsService.getUsageRestrictionsFromJson(usageRestrictionsString);

    SettingAttribute.Builder settingAttribute =
        aSettingAttribute()
            .withUuid(attrId)
            .withName(name)
            .withAccountId(accountId)
            .withAppId(appId)
            .withCategory(SettingCategory.getCategory(SettingVariableTypes.valueOf(type)))
            .withUsageRestrictions(usageRestrictionsFromJson);
    if (value != null) {
      ((EncryptableSetting) value).setAccountId(accountId);
      ((EncryptableSetting) value).setDecrypted(true);
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

  /**
   * Delete all git connectors except the ones to retain
   * @param appId Harness App ID
   * @param accountId Harness Account ID
   * @param gitConnectorsToRetain Body should be a list of git connector IDs
   * @return Rest response
   */
  @DELETE
  public RestResponse retainSelectedGitConnectorsAndDeleteRest(
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @QueryParam("accountId") @NotEmpty String accountId, List<String> gitConnectorsToRetain) {
    if (EmptyPredicate.isEmpty(gitConnectorsToRetain)) {
      // We won't delete anything if list to retain is empty
      // We expect the user to retain at least 1 Git Config
      return new RestResponse();
    }
    logger.info("Retaining the following git connectors for accountId {}: {}", accountId, gitConnectorsToRetain);
    logger.info("Starting deletion of git connectors for accountId {}", accountId);
    boolean gitConnectorsDeleted =
        settingsService.retainSelectedGitConnectorsAndDeleteRest(accountId, gitConnectorsToRetain);
    if (gitConnectorsDeleted) {
      logger.info("Deleted remaining of Git Connectors for accountId {}", accountId);
    }
    boolean yamlGitConfigsDeleted =
        yamlGitService.retainYamlGitConfigsOfSelectedGitConnectorsAndDeleteRest(accountId, gitConnectorsToRetain);
    if (yamlGitConfigsDeleted) {
      logger.info("Deleted Yaml Git Configs of accountId {} of remaining git connectors");
    }
    logger.info("Completed processing git connector and yaml git config deletions for accountId {}", accountId);
    return new RestResponse();
  }

  @POST
  @Path("validate-gcp-connectivity")
  @Consumes(MULTIPART_FORM_DATA)
  @Timed
  @ExceptionMetered
  public RestResponse<ValidationResult> validateGcpConnectivity(@QueryParam("attrId") String attrId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @QueryParam("accountId") String accountId,
      @FormDataParam("type") String type, @FormDataParam("name") String name,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {
    char[] credentials = IOUtils.toString(uploadedInputStream, Charset.defaultCharset()).toCharArray();
    SettingValue value = null;
    if (GCP.name().equals(type)) {
      if (credentials.length > 0) {
        value = GcpConfig.builder().serviceAccountKeyFileContent(credentials).build();
      } else {
        value = GcpConfig.builder().serviceAccountKeyFileContent(ENCRYPTED_FIELD_MASK.toCharArray()).build();
      }

      ((EncryptableSetting) value).setAccountId(accountId);
      ((EncryptableSetting) value).setDecrypted(true);
    }

    SettingAttribute.Builder settingAttribute =
        aSettingAttribute()
            .withUuid(attrId)
            .withName(name)
            .withAccountId(accountId)
            .withAppId(appId)
            .withCategory(SettingCategory.getCategory(SettingVariableTypes.valueOf(type)))
            .withValue(value);

    return new RestResponse<>(settingsService.validateConnectivity(settingAttribute.build()));
  }

  /**
   * Gets jobs.
   *
   * @param settingId the setting id
   * @return the jobs
   */
  @GET
  @Path("build-sources/jobs")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<JobDetails>> getJobs(
      @QueryParam("settingId") String settingId, @QueryParam("parentJobName") String parentJobName) {
    return new RestResponse<>(buildSourceService.getJobs(settingId, parentJobName));
  }

  /**
   * Gets artifact paths.
   *
   * @param jobName   the job name
   * @param settingId the setting id
   * @return the artifact paths
   */
  @GET
  @Path("build-sources/jobs/{jobName}/paths")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<String>> getArtifactPaths(@PathParam("jobName") String jobName,
      @QueryParam("settingId") String settingId, @QueryParam("groupId") String groupId,
      @QueryParam("streamType") String streamType, @QueryParam("repositoryFormat") String repositoryFormat) {
    if (isNotEmpty(repositoryFormat)) {
      return new RestResponse<>(buildSourceService.getArtifactPathsForRepositoryFormat(
          jobName, settingId, groupId, streamType, repositoryFormat));
    }
    return new RestResponse<>(buildSourceService.getArtifactPaths(jobName, settingId, groupId, streamType));
  }

  @GET
  @Path("build-sources/nexus/repositories/{repositoryName}/packageNames")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<String>> fetchPackageNames(@PathParam("repositoryName") String repositoryName,
      @QueryParam("repositoryFormat") String repositoryFormat, @QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.fetchNexusPackageNames(repositoryName, repositoryFormat, settingId));
  }

  /**
   * Gets bamboo plans.
   *
   * @param settingId the setting id
   * @return the bamboo plans
   */
  @GET
  @Path("build-sources/plans")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getBuildPlans(@QueryParam("settingId") String settingId,
      @QueryParam("streamType") String streamType, @QueryParam("repositoryType") String repositoryType,
      @QueryParam("repositoryFormat") String repositoryFormat) {
    if (repositoryFormat != null) {
      return new RestResponse<>(buildSourceService.getPlansForRepositoryFormat(
          settingId, streamType, RepositoryFormat.valueOf(repositoryFormat)));
    }
    if (repositoryType != null) {
      return new RestResponse<>(
          buildSourceService.getPlansForRepositoryType(settingId, streamType, RepositoryType.valueOf(repositoryType)));
    }
    return new RestResponse<>(buildSourceService.getPlans(settingId, streamType));
  }

  /**
   * Gets artifact paths.
   *
   * @param jobName   the job name
   * @param settingId the setting id
   * @return group Ids
   */
  @GET
  @Path("build-sources/jobs/{jobName}/groupIds")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<String>> getGroupIds(@PathParam("jobName") String jobName,
      @QueryParam("settingId") String settingId, @QueryParam("repositoryFormat") String repositoryFormat) {
    if (isNotEmpty(repositoryFormat)) {
      return new RestResponse<>(
          buildSourceService.getGroupIdsForRepositoryFormat(jobName, settingId, repositoryFormat));
    }
    return new RestResponse<>(buildSourceService.getGroupIds(jobName, settingId));
  }

  /***
   * Collects an artifact
   * @param artifactStreamId
   * @param buildDetails
   * @return
   */
  @POST
  @Path("build-sources")
  @Timed
  @ExceptionMetered
  public RestResponse<Artifact> collectArtifact(
      @QueryParam("artifactStreamId") String artifactStreamId, BuildDetails buildDetails) {
    return new RestResponse<>(buildSourceService.collectArtifact(artifactStreamId, buildDetails));
  }

  /**
   * Gets builds.
   *
   * @param artifactStreamId the artifact source id
   * @param settingId        the setting id
   * @return the builds
   * @return the builds
   */
  @GET
  @Path("build-sources/builds")
  @Timed
  @ExceptionMetered
  public RestResponse<List<BuildDetails>> getBuilds(@QueryParam("artifactStreamId") String artifactStreamId,
      @QueryParam("settingId") String settingId, @DefaultValue("-1") @QueryParam("maxResults") int maxResults) {
    List<BuildDetails> buildDetails = buildSourceService.getBuilds(artifactStreamId, settingId, maxResults);
    buildDetails = buildDetails.stream().sorted(new BuildDetailsComparator()).collect(toList());
    return new RestResponse<>(buildDetails);
  }

  @GET
  @Path("subscriptions")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> listSubscriptions(
      @QueryParam("accountId") String accountId, @QueryParam("settingId") String settingId) {
    return new RestResponse(azureResourceService.listSubscriptions(settingId));
  }

  @GET
  @Path("subscriptions/{subscriptionId}/containerRegistries")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listContainerRegistries(@QueryParam("accountId") String accountId,
      @QueryParam("settingId") String settingId, @PathParam(value = "subscriptionId") String subscriptionId) {
    return new RestResponse(azureResourceService.listContainerRegistries(settingId, subscriptionId));
  }

  @GET
  @Path("subscriptions/{subscriptionId}/containerRegistries/{registryName}/repositories")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listRepositories(@QueryParam("accountId") String accountId,
      @QueryParam("settingId") String settingId, @PathParam(value = "subscriptionId") String subscriptionId,
      @PathParam(value = "registryName") String registryName) {
    return new RestResponse(azureResourceService.listRepositories(settingId, subscriptionId, registryName));
  }

  /**
   * List.
   *
   * @param accountId                the account id
   * @return the rest response
   */
  @GET
  @Path("aws-regions")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NameValuePair>> listAwsRegions(@QueryParam("accountId") String accountId) {
    return new RestResponse(awsHelperResourceService.getAwsRegions());
  }
  /**
   * Get GCS projects
   *
   * @param settingId the setting id
   * @return the project for the service account
   */
  @GET
  @Path("build-sources/project")
  @Timed
  @ExceptionMetered
  public RestResponse<String> getProject(@QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getProject(settingId));
  }

  /**
   * Get GCS buckets
   *
   * @param projectId GCS project Id
   * @param settingId the setting id
   * @return list of buckets
   */
  @GET
  @Path("build-sources/buckets")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getBuckets(
      @QueryParam("projectId") @NotEmpty String projectId, @QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getBuckets(projectId, settingId));
  }

  /**
   * Get SMB artifact paths.
   *
   * @param settingId the setting id
   * @return the artifact paths
   */
  @GET
  @Path("build-sources/smb-paths")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getSmbPaths(@QueryParam("settingId") String settingId) {
    return new RestResponse<>(buildSourceService.getSmbPaths(settingId));
  }

  /**
   * Get SFTP artifact paths.
   *
   * @param settingId the setting id
   * @return the artifact paths
   */
  @GET
  @Path("build-sources/artifact-paths")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getSftpPaths(
      @QueryParam("settingId") String settingId, @QueryParam("streamType") String streamType) {
    return new RestResponse<>(buildSourceService.getArtifactPathsByStreamType(settingId, streamType));
  }

  /**
   * Save rest response.
   *
   * @param artifactStream the artifact stream
   * @return the rest response
   */
  @POST
  @Path("artifact-streams")
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> save(ArtifactStream artifactStream) {
    artifactStream.setAppId(GLOBAL_APP_ID);
    return new RestResponse<>(artifactStreamService.create(artifactStream));
  }

  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Path("artifact-streams")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<ArtifactStream>> listArtifactStreams(@QueryParam("currentAppId") String currentAppId,
      @QueryParam("currentEnvId") String currentEnvId, @QueryParam("accountId") String accountId,
      @QueryParam("settingId") String settingId, @QueryParam("withArtifactCount") boolean withArtifactCount,
      @QueryParam("artifactSearchString") String artifactSearchString,
      @BeanParam PageRequest<ArtifactStream> pageRequest) {
    if (settingId != null) {
      SettingAttribute settingAttribute = settingsService.get(settingId);
      if (settingAttribute == null || !settingAttribute.getAccountId().equals(accountId)
          || isEmpty(settingsService.getFilteredSettingAttributes(
                 Collections.singletonList(settingAttribute), currentAppId, currentEnvId))) {
        throw new InvalidRequestException("Setting attribute does not exist", USER);
      }
    }
    return new RestResponse<>(
        artifactStreamService.list(pageRequest, accountId, withArtifactCount, artifactSearchString));
  }

  /**
   * Gets the.
   *
   * @param streamId the stream id
   * @return the rest response
   */
  @GET
  @Path("artifact-streams/{streamId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> get(@PathParam("streamId") String streamId) {
    return new RestResponse<>(artifactStreamService.get(streamId));
  }

  /**
   * Update rest response.
   *
   * @param streamId       the stream id
   * @param artifactStream the artifact stream
   * @return the rest response
   */
  @PUT
  @Path("artifact-streams/{streamId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> update(@PathParam("streamId") String streamId, ArtifactStream artifactStream) {
    artifactStream.setUuid(streamId);
    artifactStream.setAppId(GLOBAL_APP_ID);
    return new RestResponse<>(artifactStreamService.update(artifactStream));
  }

  /**
   * Delete.
   *
   * @param id    the id
   * @return the rest response
   */
  @DELETE
  @Path("artifact-streams/{id}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@PathParam("id") String id) {
    return new RestResponse<>(artifactStreamService.delete(id, false));
  }

  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Path("artifact-streams/artifacts")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Artifact>> listArtifacts(
      @QueryParam("accountId") String accountId, @BeanParam PageRequest<Artifact> pageRequest) {
    return new RestResponse<>(artifactService.listSortByBuildNo(pageRequest));
  }

  @GET
  @Path("tags")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<String>> listTags(@QueryParam("region") String region,
      @QueryParam("computeProviderId") String settingId, @QueryParam("resourceType") String resourceType) {
    return new RestResponse<>(awsHelperResourceService.listTags(settingId, region, resourceType));
  }
}
