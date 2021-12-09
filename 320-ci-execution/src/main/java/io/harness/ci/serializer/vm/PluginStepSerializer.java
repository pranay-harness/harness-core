package io.harness.ci.serializer.vm;

import com.fasterxml.jackson.databind.JsonNode;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.yaml.extended.reports.JUnitTestReport;
import io.harness.beans.yaml.extended.reports.UnitTestReportType;
import io.harness.ci.serializer.SerializerUtils;
import io.harness.delegate.beans.ci.vm.steps.VmJunitTestReport;
import io.harness.delegate.beans.ci.vm.steps.VmPluginStep;
import io.harness.delegate.beans.ci.vm.steps.VmRunTestStep;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.TimeoutUtils;
import io.harness.yaml.core.timeout.Timeout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.harness.beans.serializer.RunTimeInputHandler.resolveJsonNodeMapParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveMapParameter;
import static io.harness.common.CIExecutionConstants.PLUGIN_ENV_PREFIX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

public class PluginStepSerializer {
    public VmPluginStep serialize(
            PluginStepInfo pluginStepInfo, String identifier, ParameterField<Timeout> parameterFieldTimeout, String stepName) {
        Map<String, JsonNode> settings =
                resolveJsonNodeMapParameter("settings", "Plugin", identifier, pluginStepInfo.getSettings(), false);
        Map<String, String> envVars = new HashMap<>();
        if (!isEmpty(settings)) {
            for (Map.Entry<String, JsonNode> entry : settings.entrySet()) {
                String key = PLUGIN_ENV_PREFIX + entry.getKey().toUpperCase();
                envVars.put(key, SerializerUtils.convertJsonNodeToString(entry.getKey(), entry.getValue()));
            }
        }

        String image =
                RunTimeInputHandler.resolveStringParameter("Image", stepName, identifier, pluginStepInfo.getImage(), false);
        long timeout = TimeoutUtils.getTimeoutInSeconds(parameterFieldTimeout, pluginStepInfo.getDefaultTimeout());

        VmPluginStep.VmPluginStepBuilder pluginStepBuilder = VmPluginStep.builder()
                .image(image)
                .envVariables(envVars)
                .timeoutSecs(timeout);

        if (pluginStepInfo.getReports() != null) {
            if (pluginStepInfo.getReports().getType() == UnitTestReportType.JUNIT) {
                JUnitTestReport junitTestReport = (JUnitTestReport) pluginStepInfo.getReports().getSpec();
                List<String> resolvedReport = junitTestReport.resolve(identifier, stepName);

                pluginStepBuilder.unitTestReport(VmJunitTestReport.builder().paths(resolvedReport).build());
            }
        }
        return pluginStepBuilder.build();
    }
}
