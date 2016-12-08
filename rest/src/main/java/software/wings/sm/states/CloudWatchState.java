package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * Created by anubhaw on 12/7/16.
 */
public class CloudWatchState extends State {
  @Transient private static final Logger logger = LoggerFactory.getLogger(CloudWatchState.class);

  @Attributes(required = true, title = "Namespace") private String namespace;
  @Attributes(required = true, title = "MetricName") private String metricName;
  @Attributes(title = "pNN.NN", description = "Percentile ") private String percentile;

  @Attributes(title = "Time duration (in minutes)", description = "Default 10 minutes") private String timeDuration;

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public CloudWatchState(String name) {
    super(name, StateType.CLOUD_WATCH.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return null;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
