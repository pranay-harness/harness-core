package software.wings.search.entities.pipeline;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.dl.WingsPersistence;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchEntity;

@Slf4j
public class PipelineSearchEntity implements SearchEntity<Pipeline> {
  @Inject private PipelineChangeHandler pipelineChangeHandler;
  @Inject private WingsPersistence wingsPersistence;

  public static final String TYPE = "pipelines";
  public static final String VERSION = "0.1";
  public static final Class<Pipeline> SOURCE_ENTITY_CLASS = Pipeline.class;
  private static final String CONFIGURATION_PATH = "pipeline/PipelineSchema.json";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public Class<Pipeline> getSourceEntityClass() {
    return SOURCE_ENTITY_CLASS;
  }

  @Override
  public String getConfigurationPath() {
    return CONFIGURATION_PATH;
  }

  @Override
  public ChangeHandler getChangeHandler() {
    return pipelineChangeHandler;
  }

  @Override
  public PipelineView getView(Pipeline pipeline) {
    PipelineView pipelineView = PipelineView.fromPipeline(pipeline);
    Application application = wingsPersistence.get(Application.class, pipeline.getAppId());
    pipelineView.setAppName(application.getName());
    return pipelineView;
  }
}
