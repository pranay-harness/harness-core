package io.harness.engine;

import static io.harness.security.dto.PrincipalType.USER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.network.SafeHttpCall;
import io.harness.ng.core.user.UserInfo;
import io.harness.opaclient.OpaServiceClient;
import io.harness.opaclient.OpaUtils;
import io.harness.opaclient.model.OpaConstants;
import io.harness.opaclient.model.OpaEvaluationResponseHolder;
import io.harness.opaclient.model.OpaPolicyEvaluationResponse;
import io.harness.opaclient.model.OpaPolicySetEvaluationResponse;
import io.harness.opaclient.model.PipelineOpaEvaluationContext;
import io.harness.opaclient.model.UserOpaEvaluationContext;
import io.harness.pms.PmsFeatureFlagService;
import io.harness.pms.contracts.governance.GovernanceMetadata;
import io.harness.pms.contracts.governance.PolicyMetadata;
import io.harness.pms.contracts.governance.PolicySetMetadata;
import io.harness.remote.client.RestClientUtils;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.UserPrincipal;
import io.harness.user.remote.UserClient;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class GovernanceServiceImpl implements GovernanceService {
  private final PmsFeatureFlagService pmsFeatureFlagService;
  private final OpaServiceClient opaServiceClient;
  private final UserClient userClient;

  @Override
  public GovernanceMetadata evaluateGovernancePolicies(String yaml, String accountId, String action) {
    if (!pmsFeatureFlagService.isEnabled(accountId, FeatureName.OPA_PIPELINE_GOVERNANCE)) {
      return GovernanceMetadata.newBuilder()
          .setDeny(false)
          .setMessage(
              String.format("FF: [%s] is disabled for account: [%s]", FeatureName.OPA_PIPELINE_GOVERNANCE, accountId))
          .build();
    }
    PipelineOpaEvaluationContext context;
    try {
      context = createEvaluationContext(yaml, action);
    } catch (IOException ex) {
      log.error("Could not create OPA evaluation context", ex);
      return GovernanceMetadata.newBuilder()
          .setDeny(true)
          .setMessage(String.format("Could not create OPA context: [%s]", ex.getMessage()))
          .build();
    }
    OpaEvaluationResponseHolder response;
    try {
      response = SafeHttpCall.executeWithExceptions(opaServiceClient.evaluateWithCredentials(
          OpaConstants.OPA_EVALUATION_TYPE_PIPELINE, "", "", "", action, context));
    } catch (Exception ex) {
      log.error("Exception while evluating OPA rules", ex);
      throw new InvalidRequestException(ex.getMessage());
    }
    return mapResponseToMetadata(response);
  }

  private GovernanceMetadata mapResponseToMetadata(OpaEvaluationResponseHolder response) {
    return GovernanceMetadata.newBuilder()
        .setId(response.getId())
        .setDeny(response.getSummary().isDeny())
        .setTimestamp(System.currentTimeMillis())
        .addAllDetails(mapPolicySetMetadata(response.getSummary().getDetails()))
        .build();
  }

  private List<PolicySetMetadata> mapPolicySetMetadata(List<OpaPolicySetEvaluationResponse> policySetResponse) {
    if (EmptyPredicate.isEmpty(policySetResponse)) {
      return Collections.emptyList();
    }
    List<PolicySetMetadata> policySetMetadataList = new ArrayList<>();
    for (OpaPolicySetEvaluationResponse setEvaluationResponse : policySetResponse) {
      policySetMetadataList.add(PolicySetMetadata.newBuilder()
                                    .setDeny(setEvaluationResponse.isDeny())
                                    .setPolicySetId(setEvaluationResponse.getPolicyset_id())
                                    .setPolicySetName(setEvaluationResponse.getPolicyset_name())
                                    .addAllPolicyMetadata(mapPolicyMetadata(setEvaluationResponse.getPolicy_details()))
                                    .build());
    }
    return policySetMetadataList;
  }

  private List<PolicyMetadata> mapPolicyMetadata(List<OpaPolicyEvaluationResponse> policyEvaluationResponses) {
    if (EmptyPredicate.isEmpty(policyEvaluationResponses)) {
      return Collections.emptyList();
    }
    List<PolicyMetadata> policyMetadataList = new ArrayList<>();
    for (OpaPolicyEvaluationResponse policyEvaluationResponse : policyEvaluationResponses) {
      policyMetadataList.add(PolicyMetadata.newBuilder()
                                 .setPolicyId(policyEvaluationResponse.getPolicy().getId())
                                 .setPolicyName(policyEvaluationResponse.getPolicy().getName())
                                 .setSeverity(policyEvaluationResponse.getPolicy().getSeverity())
                                 .addAllDenyMessages(policyEvaluationResponse.getDeny_messages())
                                 .build());
    }
    return policyMetadataList;
  }

  private PipelineOpaEvaluationContext createEvaluationContext(String yaml, String action) throws IOException {
    return PipelineOpaEvaluationContext.builder()
        .action(action)
        .pipeline(OpaUtils.extractObjectFromYamlString(yaml, OpaConstants.OPA_EVALUATION_TYPE_PIPELINE))
        .user(extractUserFromSecurityContext())
        .date(new Date())
        .build();
  }

  private UserOpaEvaluationContext extractUserFromSecurityContext() {
    if (SourcePrincipalContextBuilder.getSourcePrincipal() == null
        || !USER.equals(SourcePrincipalContextBuilder.getSourcePrincipal().getType())) {
      return null;
    }
    UserPrincipal userPrincipal = (UserPrincipal) SourcePrincipalContextBuilder.getSourcePrincipal();
    String userId = userPrincipal.getName();
    Optional<UserInfo> userOptional = RestClientUtils.getResponse(userClient.getUserById(userId));
    if (!userOptional.isPresent()) {
      return null;
    }
    UserInfo user = userOptional.get();
    return UserOpaEvaluationContext.builder().email(user.getEmail()).name(user.getName()).build();
  }
}
