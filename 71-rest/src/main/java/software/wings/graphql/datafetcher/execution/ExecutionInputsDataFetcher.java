package software.wings.graphql.datafetcher.execution;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.harness.beans.PageRequest;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.datafetcher.service.ServiceController;
import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.query.QLServiceInputsForExecutionParams;
import software.wings.graphql.schema.type.QLService;
import software.wings.graphql.schema.type.QLService.QLServiceBuilder;
import software.wings.graphql.schema.type.execution.QLExecutionInputs;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ExecutionInputsDataFetcher
    extends AbstractObjectDataFetcher<QLExecutionInputs, QLServiceInputsForExecutionParams> {
  @Inject WorkflowExecutionController workflowExecutionController;
  @Inject PipelineExecutionController pipelineExecutionController;
  @Inject ServiceResourceService serviceResourceService;
  @Inject AppService appService;
  private static final ObjectMapper MAPPER = new ObjectMapper();
  public static final String APPLICATION_DOES_NOT_EXIST_MSG = "Application does not exist";

  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLExecutionInputs fetch(QLServiceInputsForExecutionParams parameters, String accountId) {
    try (AutoLogContext ignore0 = new AccountLogContext(accountId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      validateAppBelongsToAccount(parameters, accountId);
      QLExecutionType executionType = parameters.getExecutionType();
      List<String> serviceIds;
      switch (executionType) {
        case PIPELINE:
          serviceIds = pipelineExecutionController.getArtifactNeededServices(parameters);
          break;
        case WORKFLOW:
          serviceIds = workflowExecutionController.getArtifactNeededServices(parameters);
          break;
        default:
          throw new UnsupportedOperationException("Unsupported execution type: " + executionType);
      }

      if (isEmpty(serviceIds)) {
        return QLExecutionInputs.builder().serviceInputs(new ArrayList<>()).build();
      }
      PageRequest<Service> pageRequest = aPageRequest()
                                             .addFilter(ServiceKeys.appId, EQ, parameters.getApplicationId())
                                             .addFilter(ServiceKeys.accountId, EQ, accountId)
                                             .addFilter("_id", IN, serviceIds.toArray())
                                             .build();
      List<Service> services = serviceResourceService.list(pageRequest, true, false, false, null);
      List<QLService> qlServices = new ArrayList<>();
      for (Service service : services) {
        QLServiceBuilder builder = QLService.builder();
        ServiceController.populateService(service, builder);
        qlServices.add(builder.build());
      }
      return QLExecutionInputs.builder().serviceInputs(qlServices).build();
    }
  }

  static {
    MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    MAPPER.registerModule(new Jdk8Module());
    MAPPER.registerModule(new GuavaModule());
    MAPPER.registerModule(new JavaTimeModule());
  }

  @Override
  protected QLServiceInputsForExecutionParams convertToObject(
      Map<String, Object> map, Class<QLServiceInputsForExecutionParams> clazz) {
    return MAPPER.convertValue(map, clazz);
  }

  private void validateAppBelongsToAccount(QLServiceInputsForExecutionParams params, String accountId) {
    String accountIdFromApp = appService.getAccountIdByAppId(params.getApplicationId());
    if (!accountIdFromApp.equals(accountId)) {
      throw new InvalidRequestException(APPLICATION_DOES_NOT_EXIST_MSG, WingsException.USER);
    }
  }
}