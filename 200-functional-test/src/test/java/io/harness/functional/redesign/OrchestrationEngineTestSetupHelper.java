package io.harness.functional.redesign;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.ambiance.Ambiance;
import io.harness.data.Outcome;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.functional.redesign.mixins.adviserobtainment.AdviserObtainmentTestMixin;
import io.harness.functional.redesign.mixins.advisertype.AdviserTypeTestMixin;
import io.harness.functional.redesign.mixins.ambiance.AmbianceTestMixin;
import io.harness.functional.redesign.mixins.outcome.OutcomeTestMixin;
import io.harness.functional.redesign.mixins.stepparameters.StepParametersTestMixin;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.InterruptKeys;
import io.harness.pms.advisers.AdviserObtainment;
import io.harness.pms.advisers.AdviserType;
import io.harness.rest.RestResponse;
import io.harness.state.io.StepParameters;
import io.harness.testframework.framework.Setup;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.awaitility.Awaitility;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.GenericType;

public class OrchestrationEngineTestSetupHelper {
  @Inject @Named("orchestrationMongoTemplate") private MongoTemplate mongoTemplate;

  public RequestSpecification getPortalRequestSpecification(String bearerToken) {
    return Setup.portal()
        .config(RestAssuredConfig.config()
                    .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory((cls, charset) -> {
                      ObjectMapper mapper = new ObjectMapper();
                      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                      mapper.addMixIn(Outcome.class, OutcomeTestMixin.class);
                      mapper.addMixIn(StepParameters.class, StepParametersTestMixin.class);
                      mapper.addMixIn(Ambiance.class, AmbianceTestMixin.class);
                      mapper.addMixIn(AdviserType.class, AdviserTypeTestMixin.class);
                      mapper.addMixIn(AdviserObtainment.class, AdviserObtainmentTestMixin.class);
                      return mapper;
                    }))
                    .sslConfig(new SSLConfig().relaxedHTTPSValidation()))
        .auth()
        .oauth2(bearerToken);
  }

  public PlanExecution executePlan(String bearerToken, String accountId, String appId, String planType) {
    PlanExecution original = startPlanExecution(bearerToken, accountId, appId, planType);

    final String finalStatusEnding = "ED";
    Awaitility.await().atMost(5, TimeUnit.MINUTES).pollInterval(10, TimeUnit.SECONDS).until(() -> {
      final PlanExecution planExecution = getPlanExecution(original.getUuid());
      return planExecution != null && planExecution.getStatus().name().endsWith(finalStatusEnding);
    });

    return getPlanExecution(original.getUuid());
  }

  private PlanExecution startPlanExecution(String bearerToken, String accountId, String appId, String planType) {
    GenericType<RestResponse<PlanExecution>> returnType = new GenericType<RestResponse<PlanExecution>>() {};

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("accountId", accountId);
    queryParams.put("appId", appId);

    RestResponse<PlanExecution> response = getPortalRequestSpecification(bearerToken)
                                               .queryParams(queryParams)
                                               .contentType(ContentType.JSON)
                                               .get("/execute2/" + planType)
                                               .as(returnType.getType());

    return response.getResource();
  }

  public PlanExecution getPlanExecution(String uuid) {
    Query query = query(where(PlanExecutionKeys.uuid).is(uuid));
    query.fields().include(PlanExecutionKeys.status);
    return mongoTemplate.findOne(query, PlanExecution.class);
  }

  public List<NodeExecution> getNodeExecutions(String planExecutionId) {
    Query query = query(where(NodeExecutionKeys.planExecutionId).is(planExecutionId))
                      .with(Sort.by(Sort.Direction.DESC, NodeExecutionKeys.createdAt));
    return mongoTemplate.find(query, NodeExecution.class);
  }

  public List<Interrupt> getPlanInterrupts(String planExecutionId) {
    return mongoTemplate.find(query(where(InterruptKeys.planExecutionId).is(planExecutionId)), Interrupt.class);
  }
}
