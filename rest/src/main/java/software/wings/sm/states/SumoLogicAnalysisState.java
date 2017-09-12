package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.logz.LogzSettingProvider;
import software.wings.service.impl.sumo.SumoSettingProvider;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.Set;

/**
 * Created by sriram_parthasarathy on 9/11/17.
 */
public class SumoLogicAnalysisState extends AbstractLogAnalysisState {
  @SchemaIgnore @Transient private static final Logger logger = LoggerFactory.getLogger(SumoLogicAnalysisState.class);

  @Attributes(required = true, title = "Sumo Logic Server") protected String analysisServerConfigId;

  @Attributes(required = true, title = "Search api access Id") protected String accessId;

  @Attributes(required = true, title = "Search api access Key") protected String accessKey;

  public SumoLogicAnalysisState(String name) {
    super(name, StateType.SUMO.getName());
  }

  @Override
  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (StringUtils.isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Attributes(required = true, title = "Search Keywords")
  @DefaultValue("*exception*")
  public String getQuery() {
    return query;
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  @EnumData(enumDataProvider = SumoSettingProvider.class)
  @Attributes(required = true, title = "Sumo Logic Server")
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  @Override
  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, String correlationId, Set<String> hosts) {
    return null;
  }
}
