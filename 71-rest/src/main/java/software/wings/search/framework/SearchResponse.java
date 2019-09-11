package software.wings.search.framework;

import lombok.Getter;
import lombok.Setter;
import software.wings.search.entities.application.ApplicationView;
import software.wings.search.entities.pipeline.PipelineView;

import java.util.List;

@Getter
@Setter
public class SearchResponse {
  List<ApplicationView> applications;
  List<PipelineView> pipelines;
}
