package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.appdynamics.AppDynamicsSettingProvider;
import software.wings.service.impl.newrelic.NewRelicSettingProvider;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.Set;

/**
 * Created by rsingh on 8/28/17.
 */
public class NewRelicState extends AbstractAnalysisState {
  @Transient @SchemaIgnore private static final Logger logger = LoggerFactory.getLogger(NewRelicState.class);

  @EnumData(enumDataProvider = NewRelicSettingProvider.class)
  @Attributes(required = true, title = "New Relic Server")
  private String analysisServerConfigId;

  public NewRelicState(String name) {
    super(name, StateType.NEW_RELIC.name());
  }

  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (StringUtils.isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  @Override
  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  @Override
  protected void triggerAnalysisDataCollection(ExecutionContext context, Set<String> hosts) {}

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return null;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
