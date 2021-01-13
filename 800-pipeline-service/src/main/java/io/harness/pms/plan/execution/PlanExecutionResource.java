package io.harness.pms.plan.execution;

import static io.harness.pms.contracts.plan.TriggerType.MANUAL;

import io.harness.NGCommonEntityConstants;
import io.harness.engine.OrchestrationService;
import io.harness.execution.PlanExecution;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.plan.Plan;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.ngpipeline.inputset.beans.resource.MergeInputSetRequestDTOPMS;
import io.harness.pms.plan.creation.PlanCreatorMergeService;
import io.harness.pms.plan.execution.beans.dto.InterruptDTO;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.pms.yaml.YamlUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import java.util.HashMap;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Slf4j
@Api("/pipeline/execute")
@Path("/pipeline/execute")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class PlanExecutionResource {
  public static final TriggeredBy EMBEDDED_USER = TriggeredBy.newBuilder()
                                                      .setUuid("lv0euRhKRCyiXWzS7pOg6g")
                                                      .putExtraInfo("email", "admin@harness.io")
                                                      .setIdentifier("Admin")
                                                      .build();
  public static final TriggeredBy triggeredBy = TriggeredBy.newBuilder()
                                                    .setUuid("lv0euRhKRCyiXWzS7pOg6g")
                                                    .putExtraInfo("email", "admin@harness.io")
                                                    .setIdentifier("Admin")
                                                    .build();
  private static final String pipelineYaml = "pipeline:\n"
      + "        identifier: p1\n"
      + "        name: pipeline1\n"
      + "        stages:\n"
      + "          - stage:\n"
      + "              identifier: managerDeployment\n"
      + "              type: deployment\n"
      + "              name: managerDeployment\n"
      + "              spec:\n"
      + "                service:\n"
      + "                  identifier: manager\n"
      + "                  name: manager\n"
      + "                  serviceDefinition:\n"
      + "                    type: k8s\n"
      + "                    spec:\n"
      + "                      field11: value1\n"
      + "                      field12: value2\n"
      + "                infrastructure:\n"
      + "                  environment:\n"
      + "                    identifier: stagingInfra\n"
      + "                    type: preProduction\n"
      + "                    name: staging\n"
      + "                  infrastructureDefinition:\n"
      + "                    type: k8sDirect\n"
      + "                    tmpBool: <+abc.def>\n"
      + "                    spec:\n"
      + "                      connectorRef: pEIkEiNPSgSUsbWDDyjNKw\n"
      + "                      namespace: harness\n"
      + "                      releaseName: testingqa\n"
      + "                execution:\n"
      + "                  steps:\n"
      + "                    - step:\n"
      + "                        identifier: managerCanary\n"
      + "                        type: k8sCanary\n"
      + "                        spec:\n"
      + "                          field11: value1\n"
      + "                          field12: value2\n"
      + "                    - step:\n"
      + "                        identifier: managerVerify\n"
      + "                        type: appdVerify\n"
      + "                        spec:\n"
      + "                          field21: value1\n"
      + "                          field22: value2\n"
      + "                    - step:\n"
      + "                        identifier: managerRolling\n"
      + "                        type: k8sRolling\n"
      + "                        spec:\n"
      + "                          field31: value1\n"
      + "                          field32: value2\n";

  @Inject private final OrchestrationService orchestrationService;
  @Inject private final PlanCreatorMergeService planCreatorMergeService;
  @Inject private final PipelineExecuteHelper pipelineExecuteHelper;
  @Inject private final PMSExecutionService pmsExecutionService;

  private static final String tempPipeline = "pipeline:\n"
      + "  name: \"Manager Service Deployment\"\n"
      + "  identifier: managerServiceDeployment\n"
      + "  stages:\n"
      + "    - stage:\n"
      + "        identifier: qaStage\n"
      + "        name: \"qa stage\"\n"
      + "        type: Deployment\n"
      + "        spec:\n"
      + "          service:\n"
      + "            identifier: manager\n"
      + "            serviceDefinition:\n"
      + "              type: \"Kubernetes\"\n"
      + "              spec:\n"
      + "                artifacts:\n"
      + "                  primary:\n"
      + "                    type: Dockerhub\n"
      + "                    spec:\n"
      + "                      connectorRef: \"https://registry.hub.docker.com/\"\n"
      + "                      imagePath: \"library/nginx\"\n"
      + "                      tag: \"1.18\"\n"
      + "                manifests:   # {k8s |  values | pcf |  helmSourceRepo | helmSourceRepo | kustomize | openShift}\n"
      + "                  - manifest:\n"
      + "                      identifier: baseValues\n"
      + "                      type: K8sManifest\n"
      + "                      spec:\n"
      + "                        store:\n"
      + "                          type: Git\n"
      + "                          # Git|Local\n"
      + "                          spec:\n"
      + "                            connectorRef: eJ9pksJFQDmjq6ZFbAoR-Q\n"
      + "                            gitFetchType: Branch\n"
      + "                            branch: master\n"
      + "                            paths:\n"
      + "                              - test/spec\n"
      + "            stageOverrides:\n"
      + "              manifests:   # {k8s |  values | pcf |  helmSourceRepo | helmSourceRepo | kustomize | openShift}\n"
      + "                - manifest:\n"
      + "                    identifier: qaOverride\n"
      + "                    type: Values\n"
      + "                    spec:\n"
      + "                      store:\n"
      + "                        type: Git\n"
      + "                        spec:\n"
      + "                          connectorRef: eJ9pksJFQDmjq6ZFbAoR-Q\n"
      + "                          gitFetchType: Branch\n"
      + "                          branch: master\n"
      + "                          paths:\n"
      + "                            - test/qa/values_1.yaml\n"
      + "              artifacts:\n"
      + "                primary:\n"
      + "                  type: Dockerhub\n"
      + "                  spec:\n"
      + "                    tag: \"1.18\"\n"
      + "          infrastructure:\n"
      + "            environment:\n"
      + "              identifier: stagingInfra\n"
      + "              type: PreProduction\n"
      + "              tags:\n"
      + "                cloud: GCP\n"
      + "                team: cdp\n"
      + "            # Infrastructure Type. Options: kubernetes-cluster, kubernetes-direct, kubernetes-gke, ecs, data-center, etc. See Infrastructure Types. REQUIRED\n"
      + "            # Dynamic type ???\n"
      + "            infrastructureDefinition:\n"
      + "              # Infrastructure Type. Options: kubernetes-cluster, kubernetes-direct, kubernetes-gke, ecs, data-center, etc. See Infrastructure Types. REQUIRED\n"
      + "              # Dynamic type ???\n"
      + "              type: KubernetesDirect\n"
      + "              spec:\n"
      + "                # Spec for Infrastructure Type kubernetes-direct\n"
      + "                connectorRef: pEIkEiNPSgSUsbWDDyjNKw\n"
      + "                # namespace\n"
      + "                namespace: harness\n"
      + "                # release name\n"
      + "                releaseName: testingqa\n"
      + "          execution:\n"
      + "            steps:\n"
      + "              - step:\n"
      + "                  name: \"Rollout Deployment\"\n"
      + "                  identifier: rolloutDeployment1\n"
      + "                  type: K8sRollingDeploy\n"
      + "                  spec:\n"
      + "                    timeout: 120000\n"
      + "                    skipDryRun: false\n"
      + "            rollbackSteps:\n"
      + "              - step:\n"
      + "                  name: \"Rollback Rollout Deployment\"\n"
      + "                  identifier: rollbackRolloutDeployment1\n"
      + "                  type: K8sRollingRollback\n"
      + "                  spec:\n"
      + "                    timeout: 120000\n"
      + "              - step:\n"
      + "                  identifier: shellScript1\n"
      + "                  type: ShellScript\n"
      + "                  spec:\n"
      + "                    executeOnDelegate: true\n"
      + "                    connectionType: SSH\n"
      + "                    scriptType: BASH\n"
      + "                    scriptString: |\n"
      + "                      echo 'I should be executed during rollback'\n"
      + "    - stage:\n"
      + "        identifier: prodStage\n"
      + "        name: \"prod stage\"\n"
      + "        type: Deployment\n"
      + "        spec:\n"
      + "          service:\n"
      + "            useFromStage:\n"
      + "              stage: qaStage\n"
      + "            stageOverrides:\n"
      + "              manifests:   # {k8s |  values | pcf |  helmSourceRepo | helmSourceRepo | kustomize | openShift}\n"
      + "                - manifest:\n"
      + "                    identifier: prodOverride\n"
      + "                    type: Values\n"
      + "                    spec:\n"
      + "                      store:\n"
      + "                        type: Git\n"
      + "                        spec:\n"
      + "                          connectorRef: eJ9pksJFQDmjq6ZFbAoR-Q\n"
      + "                          gitFetchType: Branch\n"
      + "                          branch: master\n"
      + "                          paths:\n"
      + "                            - test/prod/values.yaml\n"
      + "              artifacts:\n"
      + "                primary:\n"
      + "                  type: Dockerhub\n"
      + "                  spec:\n"
      + "                    tag: \"1.18\"\n"
      + "          infrastructure:\n"
      + "            useFromStage:\n"
      + "              stage: qaStage\n"
      + "              overrides:\n"
      + "                environment:\n"
      + "                  identifier: prodInfra\n"
      + "                infrastructureDefinition:\n"
      + "                  type: KubernetesDirect\n"
      + "                  spec:\n"
      + "                    releaseName: testingProd\n"
      + "          execution:\n"
      + "            steps:\n"
      + "              - stepGroup:\n"
      + "                  name: StepGroup1\n"
      + "                  identifier: StepGroup1\n"
      + "                  steps:\n"
      + "                    - parallel:\n"
      + "                        - step:\n"
      + "                            name: http step 1\n"
      + "                            identifier: httpStep1\n"
      + "                            type: Http\n"
      + "                            spec:\n"
      + "                              socketTimeoutMillis: 1000\n"
      + "                              method: GET\n"
      + "                              url: http://httpstat.us/200\n"
      + "              - step:\n"
      + "                  name: \"Rollout Deployment\"\n"
      + "                  identifier: rolloutDeployment2\n"
      + "                  type: K8sRollingDeploy\n"
      + "                  spec:\n"
      + "                    timeout: 120000\n"
      + "                    skipDryRun: false\n"
      + "            rollbackSteps:\n"
      + "              - step:\n"
      + "                  name: \"Rollback Rollout Deployment\"\n"
      + "                  identifier: rollbackRolloutDeployment2\n"
      + "                  type: K8sRollingRollback\n"
      + "                  spec:\n"
      + "                    timeout: 120000";

  private static final String manifestPipeline = "pipeline:\n"
      + "  name: P1\n"
      + "  identifier: P1\n"
      + "  description: \"\"\n"
      + "  stages:\n"
      + "    - stage:\n"
      + "        name: S1\n"
      + "        identifier: S1\n"
      + "        description: \"\"\n"
      + "        type: Deployment\n"
      + "        spec:\n"
      + "          service:\n"
      + "            identifier: nginx\n"
      + "            name: nginx\n"
      + "            description: \"\"\n"
      + "            serviceDefinition:\n"
      + "              type: Kubernetes\n"
      + "              spec:\n"
      + "                artifacts:\n"
      + "                  sidecars: []\n"
      + "                  primary:\n"
      + "                    type: Dockerhub\n"
      + "                    spec:\n"
      + "                      connectorRef: docker_public\n"
      + "                      imagePath: library/nginx\n"
      + "                      tag: stable-perl\n"
      + "                manifests:\n"
      + "                  - manifest:\n"
      + "                      identifier: k8s_test_manifest\n"
      + "                      type: K8sManifest\n"
      + "                      spec:\n"
      + "                        store:\n"
      + "                          type: Git\n"
      + "                          spec:\n"
      + "                            connectorRef: git_test\n"
      + "                            gitFetchType: Branch\n"
      + "                            branch: master\n"
      + "                            commitId: \"\"\n"
      + "                            paths:\n"
      + "                              - logs/quickstart.yaml\n"
      + "                artifactOverrideSets: []\n"
      + "                manifestOverrideSets: []\n"
      + "          infrastructure:\n"
      + "            environment:\n"
      + "              name: k8s\n"
      + "              identifier: k8s\n"
      + "              description: \"\"\n"
      + "              type: PreProduction\n"
      + "            infrastructureDefinition:\n"
      + "              type: KubernetesDirect\n"
      + "              spec:\n"
      + "                connectorRef: k8s_sa\n"
      + "                namespace: garvit-test\n"
      + "                releaseName: garvit-test-nginx\n"
      + "          execution:\n"
      + "            steps:\n"
      + "              - step:\n"
      + "                  name: http step 1\n"
      + "                  identifier: httpStep1\n"
      + "                  type: Http\n"
      + "                  spec:\n"
      + "                    socketTimeoutMillis: 1000\n"
      + "                    method: GET\n"
      + "                    url: https://www.google.com/";

  private static final String ngPipeline = "pipeline:\n"
      + "  name: P1\n"
      + "  identifier: P1\n"
      + "  description: \"\"\n"
      + "  stages:\n"
      + "    - stage:\n"
      + "        name: S1\n"
      + "        identifier: S1\n"
      + "        description: \"\"\n"
      + "        type: Deployment\n"
      + "        spec:\n"
      + "          service:\n"
      + "            identifier: nginx\n"
      + "            name: nginx\n"
      + "            description: \"\"\n"
      + "            serviceDefinition:\n"
      + "              type: Kubernetes\n"
      + "              spec:\n"
      + "                artifacts:\n"
      + "                  sidecars: []\n"
      + "                  primary:\n"
      + "                    type: Dockerhub\n"
      + "                    spec:\n"
      + "                      connectorRef: docker_public\n"
      + "                      imagePath: library/nginx\n"
      + "                      tag: stable-perl\n"
      + "                manifests: []\n"
      + "                artifactOverrideSets: []\n"
      + "                manifestOverrideSets: []\n"
      + "          infrastructure:\n"
      + "            environment:\n"
      + "              name: k8s\n"
      + "              identifier: k8s\n"
      + "              description: \"\"\n"
      + "              type: PreProduction\n"
      + "            infrastructureDefinition:\n"
      + "              type: KubernetesDirect\n"
      + "              spec:\n"
      + "                connectorRef: k8s_sa\n"
      + "                namespace: garvit-test\n"
      + "                releaseName: garvit-test-nginx\n"
      + "          execution:\n"
      + "            steps:\n"
      + "              - step:\n"
      + "                  name: http step 1\n"
      + "                  identifier: httpStep1\n"
      + "                  type: Http\n"
      + "                  spec:\n"
      + "                    socketTimeoutMillis: 1000\n"
      + "                    method: GET\n"
      + "                    url: https://www.google.com/\n";

  @GET
  @ApiOperation(value = "Execute A Pipeline", nickname = "executePipeline")
  public Response executePipeline() throws IOException {
    String processedYaml = YamlUtils.injectUuid(pipelineYaml);
    PlanCreationBlobResponse resp = planCreatorMergeService.createPlan(processedYaml);
    Plan plan = PlanExecutionUtils.extractPlan(resp);
    PlanExecution planExecution = orchestrationService.startExecution(plan,
        new HashMap<>(ImmutableMap.of("accountId", "kmpySmUISimoRrJL6NL73w", "orgIdentifier", "harness",
            "projectIdentifier", "pipeline", "expressionFunctorToken", "12345")),
        ExecutionMetadata.newBuilder()
            .setRunSequence(0)
            .setTriggerInfo(ExecutionTriggerInfo.newBuilder().setTriggerType(MANUAL).build())
            .build());
    return Response.ok(planExecution, MediaType.APPLICATION_JSON_TYPE).build();
  }

  @POST
  @Path("/{identifier}")
  @ApiOperation(
      value = "Execute a pipeline with inputSet pipeline yaml", nickname = "postPipelineExecuteWithInputSetYaml")
  public ResponseDTO<PlanExecutionResponseDto>
  runPipelineWithInputSetPipelineYaml(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam("identifier") @NotEmpty String pipelineIdentifier,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @ApiParam(hidden = true, type = "") String inputSetPipelineYaml) throws IOException {
    PlanExecution planExecution = pipelineExecuteHelper.runPipelineWithInputSetPipelineYaml(accountId, orgIdentifier,
        projectIdentifier, pipelineIdentifier, inputSetPipelineYaml, null,
        ExecutionTriggerInfo.newBuilder().setTriggerType(MANUAL).setTriggeredBy(triggeredBy).build());
    PlanExecutionResponseDto planExecutionResponseDto =
        PlanExecutionResponseDto.builder().planExecution(planExecution).build();
    return ResponseDTO.newResponse(planExecutionResponseDto);
  }

  @POST
  @Path("/{identifier}/inputSetList")
  @ApiOperation(
      value = "Execute a pipeline with input set references list", nickname = "postPipelineExecuteWithInputSetList")
  public ResponseDTO<PlanExecutionResponseDto>
  runPipelineWithInputSetIdentifierList(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam("identifier") @NotEmpty String pipelineIdentifier,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @NotNull @Valid MergeInputSetRequestDTOPMS mergeInputSetRequestDTO) throws IOException {
    ExecutionTriggerInfo triggerInfo =
        ExecutionTriggerInfo.newBuilder().setTriggerType(MANUAL).setTriggeredBy(EMBEDDED_USER).build();
    PlanExecution planExecution = pipelineExecuteHelper.runPipelineWithInputSetReferencesList(accountId, orgIdentifier,
        projectIdentifier, pipelineIdentifier, mergeInputSetRequestDTO.getInputSetReferences(), triggerInfo);
    PlanExecutionResponseDto planExecutionResponseDto =
        PlanExecutionResponseDto.builder().planExecution(planExecution).build();
    return ResponseDTO.newResponse(planExecutionResponseDto);
  }

  @PUT
  @ApiOperation(value = "pause, resume or stop the pipeline executions", nickname = "handleInterrupt")
  @Path("/interrupt/{planExecutionId}")
  public ResponseDTO<InterruptDTO> handleInterrupt(@NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgId, @NotNull @QueryParam("projectIdentifier") String projectId,
      @NotNull @QueryParam("interruptType") PlanExecutionInterruptType executionInterruptType,
      @NotNull @PathParam("planExecutionId") String planExecutionId) {
    return ResponseDTO.newResponse(pmsExecutionService.registerInterrupt(executionInterruptType, planExecutionId));
  }
}
