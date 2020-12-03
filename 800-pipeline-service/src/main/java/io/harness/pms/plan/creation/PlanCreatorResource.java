package io.harness.pms.plan.creation;

import io.harness.pms.plan.PlanCreationBlobResponse;
import io.harness.pms.plan.creation.PlanCreatorMergeService;

import com.google.inject.Inject;
import com.google.protobuf.util.JsonFormat;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Api("/plan-creator")
@Path("/plan-creator")
@Produces({"application/json"})
@Consumes({"application/json"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class PlanCreatorResource {
  private static final String pipelineYaml = "pipeline:\n"
      + "        identifier: p1\n"
      + "        name: pipeline1\n"
      + "        stages:\n"
      + "          - parallel:  \n"
      + "            - stage:\n"
      + "                identifier: managerDeployment\n"
      + "                type: deployment\n"
      + "                name: managerDeployment\n"
      + "                spec:\n"
      + "                    service:\n"
      + "                    identifier: manager\n"
      + "                    name: manager\n"
      + "                    serviceDefinition:\n"
      + "                        type: k8s\n"
      + "                        spec:\n"
      + "                        field11: value1\n"
      + "                        field12: value2\n"
      + "                    infrastructure:\n"
      + "                    environment:\n"
      + "                        identifier: stagingInfra\n"
      + "                        type: preProduction\n"
      + "                        name: staging\n"
      + "                    infrastructureDefinition:\n"
      + "                        type: k8sDirect\n"
      + "                        spec:\n"
      + "                        connectorRef: pEIkEiNPSgSUsbWDDyjNKw\n"
      + "                        namespace: harness\n"
      + "                        releaseName: testingqa\n"
      + "                    execution:\n"
      + "                    steps:\n"
      + "                        - step:\n"
      + "                            identifier: managerCanary\n"
      + "                            type: k8sCanary\n"
      + "                            spec:\n"
      + "                            field11: value1\n"
      + "                            field12: value2\n"
      + "                        - step:\n"
      + "                            identifier: managerVerify\n"
      + "                            type: appdVerify\n"
      + "                            spec:\n"
      + "                            field21: value1\n"
      + "                            field22: value2\n"
      + "                        - step:\n"
      + "                            identifier: managerRolling\n"
      + "                            type: k8sRolling\n"
      + "                            spec:\n"
      + "                            field31: value1\n"
      + "                            field32: value2\n"
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
      + "                          field32: value2";

  @Inject private PlanCreatorMergeService planCreatorMergeService;

  @GET
  @ApiOperation(value = "Get plan creation response for sample pipeline", nickname = "getPlanCreationResponse")
  public Response getPlanCreationResponse() throws IOException {
    PlanCreationBlobResponse resp = planCreatorMergeService.createPlan(pipelineYaml);
    String json = JsonFormat.printer().print(resp);
    return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
  }
}
