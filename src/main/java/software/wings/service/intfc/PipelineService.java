package software.wings.service.intfc;

import software.wings.beans.PipelineExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * Created by anubhaw on 10/26/16.
 */
public interface PipelineService {
  PageResponse<PipelineExecution> listPipelineExecutions(PageRequest<PipelineExecution> pageRequest, String appId);
}
