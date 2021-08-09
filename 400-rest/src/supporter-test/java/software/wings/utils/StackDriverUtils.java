package software.wings.utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import software.wings.sm.StateType;
import software.wings.verification.stackdriver.StackDriverMetricCVConfiguration;
import software.wings.verification.stackdriver.StackDriverMetricDefinition;

import com.google.common.base.Charsets;
import java.io.File;
import java.util.Arrays;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;

@UtilityClass
public class StackDriverUtils {
  public static StackDriverMetricCVConfiguration createStackDriverConfig(String accountId) throws Exception {
    String paramsForStackDriver = null;
    if (!System.getProperty("user.dir").contains("bin")) {
      paramsForStackDriver = FileUtils.readFileToString(
          new File("../400-rest/src/test/resources/apm/stackdriverpayload.json"), Charsets.UTF_8);
    } else {
      paramsForStackDriver = FileUtils.readFileToString(
          new File("400-rest/src/test/resources/apm/stackdriverpayload.json"), Charsets.UTF_8);
    }
    StackDriverMetricDefinition definition = StackDriverMetricDefinition.builder()
                                                 .filterJson(paramsForStackDriver)
                                                 .metricName("metricName")
                                                 .metricType("INFRA")
                                                 .txnName("TransactionName1")
                                                 .build();
    StackDriverMetricDefinition definition1 = StackDriverMetricDefinition.builder()
                                                  .filterJson(paramsForStackDriver)
                                                  .metricName("metricName2")
                                                  .metricType("INFRA")
                                                  .txnName("TransactionName2")
                                                  .build();
    StackDriverMetricDefinition definition2 = StackDriverMetricDefinition.builder()
                                                  .filterJson(paramsForStackDriver)
                                                  .metricName("metricName3")
                                                  .metricType("INFRA")
                                                  .txnName("TransactionName2")
                                                  .build();
    StackDriverMetricCVConfiguration configuration =
        StackDriverMetricCVConfiguration.builder()
            .metricDefinitions(Arrays.asList(definition, definition1, definition2))
            .build();
    configuration.setAccountId(accountId);
    configuration.setStateType(StateType.STACK_DRIVER);
    configuration.setEnvId(generateUuid());
    configuration.setName("StackDriver");
    configuration.setConnectorId(generateUuid());
    configuration.setServiceId(generateUuid());

    return configuration;
  }
}
