package io.harness.ng.core.remote;

import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_ORGANIZATION_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_PROJECT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_USERGROUP_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.ORGANIZATION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.PROJECT;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.USERGROUP;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import com.mchange.net.MailSender;
import com.mchange.net.SmtpMailSender;
import io.harness.ModuleType;
import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.AggregateOrganizationService;
import io.harness.ng.core.api.AggregateProjectService;
import io.harness.ng.core.api.AggregateUserGroupService;
import io.harness.ng.core.dto.AggregateACLRequest;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.OrganizationAggregateDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.dto.ProjectAggregateDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.UserGroupAggregateDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.services.OrganizationService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.data.mongodb.core.query.Criteria;
import retrofit2.http.Body;

@OwnedBy(PL)
@Api("aggregate")
@Path("aggregate")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class NGAggregateResource {
  private final AggregateOrganizationService aggregateOrganizationService;
  private final AggregateProjectService aggregateProjectService;
  private final AggregateUserGroupService aggregateUserGroupService;
  private final OrganizationService organizationService;
  private final AccessControlClient accessControlClient;


  @GET
  @Path("projects/{identifier}")
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  @ApiOperation(value = "Gets a ProjectAggregateDTO by identifier", nickname = "getProjectAggregateDTO")
  public ResponseDTO<ProjectAggregateDTO> get(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @DefaultValue(
          DEFAULT_ORG_IDENTIFIER) @OrgIdentifier @io.harness.ng.core.OrgIdentifier String orgIdentifier) {
    return ResponseDTO.newResponse(
        aggregateProjectService.getProjectAggregateDTO(accountIdentifier, orgIdentifier, identifier));
  }

  @POST
  @Path("sendemailnotifications")
  @ApiOperation(value = "sendemailnotifications", nickname = "sendemailnotifications")
  public String sendEmailNotifications(@Body SlackNotificationsRequest slackNotificationsRequest) throws IOException, InterruptedException {
    sendEmailNotificationsToUsers(slackNotificationsRequest);
    return "success";
  }

  @POST
  @Path("sendslacknotifications")
  @ApiOperation(value = "sendslacknotifications", nickname = "sendslacknotifications")
  public String sendSlackNotifications(@Body SlackNotificationsRequest slackNotificationsRequest) throws IOException, InterruptedException {
    Map<String, String> userSlackWebhookMap = new HashMap<String, String>();
    userSlackWebhookMap.put("akhilesh.pandey@harness.io", "https://hooks.slack.com/services/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");
    userSlackWebhookMap.put("sainath.batthala@harness.io", "https://hooks.slack.com/services/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");
    userSlackWebhookMap.put("meenakshi.raikwar@harness.io", "https://hooks.slack.com/services/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");
    userSlackWebhookMap.put("prashant.batra@harness.io", "https://hooks.slack.com/services/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");
    sendSlackNotifications(slackNotificationsRequest, userSlackWebhookMap);
    return "success";
  }

  public void sendEmailNotificationsToUsers(SlackNotificationsRequest slackNotificationsRequest) {
    DeploymentDetails deploymentDetails = slackNotificationsRequest.getDeploymentDetails();
    String deploymentStatus = deploymentDetails.getDeploymentStatus();
    for (User userObj : slackNotificationsRequest.getUsers()) {
      if (userObj.isHasHarnessAccount()) {
        continue;
      }
      String host="smtp.gmail.com";
      final String user="notificationsharness8@gmail.com\n";//change accordingly
      final String password="H@rnessH@rness";//change accordingly

      String to=userObj.getName();//change accordingly

      //Get the session object
      Properties props = new Properties();
      props.put("mail.smtp.host",host);
      props.put("mail.smtp.port",587);
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.starttls.enable", "true");

      Session session = Session.getDefaultInstance(props,
              new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                  return new PasswordAuthentication(user,password);
                }
              });

      //Compose the message
      try {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(user));
        message.addRecipient(Message.RecipientType.TO,new InternetAddress(to));
        message.setSubject("You have been invited to Harness");
        String someHtmlMessage =
                "<div dir=\"ltr\">Hi " + userObj.getName().split("@")[0] + ",</div>\n" +
                "<div dir=\"ltr\"><br />You colleague " + deploymentDetails.getTriggeredBy() + " invited you to join <a style=\"text-decoration: none;\" href=\"app.harness.io\"><strong style=\"color: #00ade4;\">Harness</strong></a></div>\n" +
                "<div dir=\"ltr\"><img style=\"background-color: black;\" src=\"https://harness.io/wp-content/themes/harnessio/assets/images/Harness-white-logo-rainbow.png\" width=\"219\" height=\"74\" /><br />Harness CI/CD Platform enables software changes of all types to reach production environments in a safe, quick, and sustainable way.<br /><br /></div>\n" +
                "<div dir=\"ltr\">Get Ship Done.</div>";
        message.setContent(someHtmlMessage, "text/html; charset=utf-8");

        //send the message
        Transport.send(message);

        System.out.println("message sent successfully...");

      } catch (MessagingException e) {
        e.printStackTrace();
      }
    }
  }

  public void sendSlackNotifications(SlackNotificationsRequest slackNotificationsRequest, Map<String, String> userSlackWebhookMap) throws IOException, InterruptedException {
    CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
    DeploymentDetails deploymentDetails = slackNotificationsRequest.getDeploymentDetails();
    String deploymentStatus = deploymentDetails.getDeploymentStatus();
    for (User userObj : slackNotificationsRequest.getUsers()) {
      if (!userObj.isHasHarnessAccount() || !userObj.isHasSlackAccount()) {
        continue;
      }
      String user = userObj.getName();
      HttpPost httpPost = new HttpPost(userSlackWebhookMap.get(user));
      httpPost.setHeader("Content-Type", "application/json");
      String json = "{\n" +
              "        \"blocks\": [\n" +
              "                {\n" +
              "                        \"type\": \"section\",\n" +
              "                        \"text\": {\n" +
              "                                \"type\": \"mrkdwn\",\n" +
              "                                \"text\": \"Hi @" + user.split("@")[0] + ",\\nFollowing deployment involving your changes is " + deploymentStatus + "\\n*<" + deploymentDetails.getPipelineExecutionLink() + "|Harness Pipeline Execution>*\"\n" +
              "                        }\n" +
              "                },\n" +
              "                {\n" +
              "                        \"type\": \"section\",\n" +
              "                        \"fields\": [\n" +
              "                                {\n" +
              "                                        \"type\": \"mrkdwn\",\n" +
              "                                        \"text\": \"*Deployment Type:*\\n" + deploymentDetails.getDeploymentType() + "\"\n" +
              "                                },\n" +
              "                                {\n" +
              "                                        \"type\": \"mrkdwn\",\n" +
              "                                        \"text\": \"*Triggered on:*\\n " + deploymentDetails.getTriggeredOn() + "\"\n" +
              "                                },\n" +
              "                                {\n" +
              "                                        \"type\": \"mrkdwn\",\n" +
              "                                        \"text\": \"*Triggered by:*\\n " + deploymentDetails.getTriggeredBy() + "\"\n" +
              "                                },\n" +
              "                                {\n" +
              "                                        \"type\": \"mrkdwn\",\n" +
              "                                        \"text\": \"*Deployment Status:*\\n " + deploymentStatus + "\"\n" +
              "                                }\n" +
              "                        ]\n" +
              "                },\n" +
              "                {\n" +
              "                        \"type\": \"actions\",\n" +
              "                        \"elements\": [\n" +
              "                                {\n" +
              "                                        \"type\": \"button\",\n" +
              "                                        \"text\": {\n" +
              "                                                \"type\": \"plain_text\",\n" +
              "                                                \"emoji\": true,\n" +
              "                                                \"text\": \"View on Harness\"\n" +
              "                                        },\n" +
              "                                        \"style\": \"primary\",\n" +
              "                                        \"value\": \"click_me_123\"\n" +
              "                                }\n" +
              "                        ]\n" +
              "                }\n" +
              "        ]\n" +
              "}";

      httpPost.setEntity(new StringEntity(json));
      closeableHttpClient.execute(httpPost);
    }
  }

  @GET
  @Path("projects")
  @ApiOperation(value = "Get ProjectAggregateDTO list", nickname = "getProjectAggregateDTOList")
  public ResponseDTO<PageResponse<ProjectAggregateDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam("hasModule") @DefaultValue("true") boolean hasModule,
      @QueryParam(NGResourceFilterConstants.MODULE_TYPE_KEY) ModuleType moduleType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(ProjectKeys.lastModifiedAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    Set<String> permittedOrgIds = getPermittedOrganizations(accountIdentifier, orgIdentifier);
    ProjectFilterDTO projectFilterDTO = getProjectFilterDTO(searchTerm, permittedOrgIds, hasModule, moduleType);
    return ResponseDTO.newResponse(getNGPageResponse(aggregateProjectService.listProjectAggregateDTO(
        accountIdentifier, getPageRequest(pageRequest), projectFilterDTO)));
  }

  private ProjectFilterDTO getProjectFilterDTO(
      String searchTerm, Set<String> orgIdentifiers, boolean hasModule, ModuleType moduleType) {
    return ProjectFilterDTO.builder()
        .searchTerm(searchTerm)
        .orgIdentifiers(orgIdentifiers)
        .hasModule(hasModule)
        .moduleType(moduleType)
        .build();
  }

  @GET
  @Path("organizations/{identifier}")
  @NGAccessControlCheck(resourceType = ORGANIZATION, permission = VIEW_ORGANIZATION_PERMISSION)
  @ApiOperation(value = "Gets an OrganizationAggregateDTO by identifier", nickname = "getOrganizationAggregateDTO")
  public ResponseDTO<OrganizationAggregateDTO> get(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    return ResponseDTO.newResponse(
        aggregateOrganizationService.getOrganizationAggregateDTO(accountIdentifier, identifier));
  }

  @GET
  @Path("organizations")
  @NGAccessControlCheck(resourceType = ORGANIZATION, permission = VIEW_ORGANIZATION_PERMISSION)
  @ApiOperation(value = "Get OrganizationAggregateDTO list", nickname = "getOrganizationAggregateDTOList")
  public ResponseDTO<PageResponse<OrganizationAggregateDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    OrganizationFilterDTO organizationFilterDTO = OrganizationFilterDTO.builder().searchTerm(searchTerm).build();
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder harnessManagedOrder =
          SortOrder.Builder.aSortOrder().withField(OrganizationKeys.harnessManaged, SortOrder.OrderType.DESC).build();
      SortOrder nameOrder =
          SortOrder.Builder.aSortOrder().withField(OrganizationKeys.name, SortOrder.OrderType.ASC).build();
      organizationFilterDTO.setIgnoreCase(true);
      pageRequest.setSortOrders(ImmutableList.of(harnessManagedOrder, nameOrder));
    }
    return ResponseDTO.newResponse(getNGPageResponse(aggregateOrganizationService.listOrganizationAggregateDTO(
        accountIdentifier, getPageRequest(pageRequest), organizationFilterDTO)));
  }

  @GET
  @Path("acl/usergroups")
  @ApiOperation(value = "Get Aggregated User Group list", nickname = "getUserGroupAggregateList")
  public ResponseDTO<PageResponse<UserGroupAggregateDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @BeanParam PageRequest pageRequest,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam("userSize") @DefaultValue("6") @Max(20) int userSize) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(USERGROUP, null), VIEW_USERGROUP_PERMISSION);
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(ProjectKeys.lastModifiedAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    return ResponseDTO.newResponse(aggregateUserGroupService.listAggregateUserGroups(
        pageRequest, accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, userSize));
  }

  @POST
  @Path("acl/usergroups/filter")
  @ApiOperation(value = "Get Aggregated User Group list with filter", nickname = "getUserGroupAggregateListsWithFilter")
  public ResponseDTO<List<UserGroupAggregateDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Body AggregateACLRequest aggregateACLRequest) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(USERGROUP, null), VIEW_USERGROUP_PERMISSION);
    return ResponseDTO.newResponse(aggregateUserGroupService.listAggregateUserGroups(
        accountIdentifier, orgIdentifier, projectIdentifier, aggregateACLRequest));
  }

  @GET
  @Path("acl/usergroups/{identifier}")
  @ApiOperation(value = "Get Aggregated User Group", nickname = "getUserGroupAggregate")
  public ResponseDTO<UserGroupAggregateDTO> list(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(USERGROUP, identifier), VIEW_USERGROUP_PERMISSION);
    return ResponseDTO.newResponse(aggregateUserGroupService.getAggregatedUserGroup(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }

  private Set<String> getPermittedOrganizations(@NotNull String accountIdentifier, String orgIdentifier) {
    Set<String> orgIdentifiers;
    if (isEmpty(orgIdentifier)) {
      Criteria orgCriteria = Criteria.where(OrganizationKeys.accountIdentifier)
                                 .is(accountIdentifier)
                                 .and(OrganizationKeys.deleted)
                                 .ne(Boolean.TRUE);
      List<Organization> organizations = organizationService.list(orgCriteria);
      orgIdentifiers = organizations.stream().map(Organization::getIdentifier).collect(Collectors.toSet());
    } else {
      orgIdentifiers = Collections.singleton(orgIdentifier);
    }

    ResourceScope resourceScope = ResourceScope.builder().accountIdentifier(accountIdentifier).build();
    List<PermissionCheckDTO> permissionChecks = orgIdentifiers.stream()
                                                    .map(oi
                                                        -> PermissionCheckDTO.builder()
                                                               .permission(VIEW_PROJECT_PERMISSION)
                                                               .resourceIdentifier(oi)
                                                               .resourceScope(resourceScope)
                                                               .resourceType(ORGANIZATION)
                                                               .build())
                                                    .collect(Collectors.toList());
    AccessCheckResponseDTO accessCheckResponse = accessControlClient.checkForAccess(permissionChecks);
    return accessCheckResponse.getAccessControlList()
        .stream()
        .filter(AccessControlDTO::isPermitted)
        .map(AccessControlDTO::getResourceIdentifier)
        .collect(Collectors.toSet());
  }
}
