package io.harness.ci.serializer;

import static io.harness.beans.serializer.RunTimeInputHandler.resolveJsonNodeMapParameter;
import static io.harness.common.CIExecutionConstants.PLUGIN_ENV_PREFIX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.Collections.emptyList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.yaml.extended.reports.JUnitTestReport;
import io.harness.beans.yaml.extended.reports.UnitTestReport;
import io.harness.beans.yaml.extended.reports.UnitTestReportType;
import io.harness.callback.DelegateCallbackToken;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.pms.yaml.ParameterField;
import io.harness.product.ci.engine.proto.PluginStep;
import io.harness.product.ci.engine.proto.Report;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.utils.TimeoutUtils;
import io.harness.yaml.core.timeout.Timeout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
@OwnedBy(HarnessTeam.CI)
public class PluginStepProtobufSerializer implements ProtobufStepSerializer<PluginStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  public UnitStep serializeStepWithStepParameters(PluginStepInfo pluginStepInfo, Integer port, String callbackId,
      String logKey, String identifier, ParameterField<Timeout> parameterFieldTimeout, String accountId,
      String stepName) {
    if (callbackId == null) {
      throw new CIStageExecutionException("CallbackId can not be null");
    }

    if (port == null) {
      throw new CIStageExecutionException("Port can not be null");
    }

    long timeout = TimeoutUtils.getTimeoutInSeconds(parameterFieldTimeout, pluginStepInfo.getDefaultTimeout());
    StepContext stepContext = StepContext.newBuilder().setExecutionTimeoutSecs(timeout).build();

    Map<String, JsonNode> settings =
        resolveJsonNodeMapParameter("settings", "Plugin", identifier, pluginStepInfo.getSettings(), false);
    Map<String, String> envVarMap = new HashMap<>();
    if (!isEmpty(settings)) {
      for (Map.Entry<String, JsonNode> entry : settings.entrySet()) {
        String key = PLUGIN_ENV_PREFIX + entry.getKey().toUpperCase();
        envVarMap.put(key, convertJsonNodeToString(entry.getKey(), entry.getValue()));
      }
    }

    PluginStep.Builder builder = PluginStep.newBuilder();

    UnitTestReport reports = pluginStepInfo.getReports();
    if (reports != null) {
      if (reports.getType() == UnitTestReportType.JUNIT) {
        JUnitTestReport junitTestReport = (JUnitTestReport) reports.getSpec();
        List<String> resolvedReport = junitTestReport.resolve(identifier, "run");

        Report report = Report.newBuilder().setType(Report.Type.JUNIT).addAllPaths(resolvedReport).build();
        builder.addReports(report);
      }
    }

    PluginStep pluginStep =
        builder.setContainerPort(port)
            .setImage(RunTimeInputHandler.resolveStringParameter(
                "Image", "Plugin", identifier, pluginStepInfo.getImage(), true))
            .addAllEntrypoint(Optional.ofNullable(pluginStepInfo.getEntrypoint()).orElse(emptyList()))
            .putAllEnvironment(envVarMap)
            .setContext(stepContext)
            .build();

    return UnitStep.newBuilder()
        .setId(identifier)
        .setTaskId(callbackId)
        .setAccountId(accountId)
        .setContainerPort(port)
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(stepName)
        .setPlugin(pluginStep)
        .setLogKey(logKey)
        .build();
  }

  private String convertJsonNodeToString(String key, JsonNode jsonNode) {
    try {
      if (jsonNode.isValueNode()) {
        return jsonNode.asText("");
      } else if (jsonNode.isArray() && isPrimitiveArray(jsonNode)) {
        ArrayNode arrayNode = (ArrayNode) jsonNode;
        List<String> strValues = new ArrayList<>();
        for (JsonNode node : arrayNode) {
          strValues.add(node.asText(""));
        }

        return String.join(",", strValues);
      } else {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(jsonNode);
      }
    } catch (Exception ex) {
      throw new CIStageExecutionException(String.format("Invalid setting attribute %s value", key));
    }
  }

  // Return whether array contains only value node or not.
  private boolean isPrimitiveArray(JsonNode jsonNode) {
    ArrayNode arrayNode = (ArrayNode) jsonNode;
    for (JsonNode e : arrayNode) {
      if (!e.isValueNode()) {
        return false;
      }
    }
    return true;
  }
}
