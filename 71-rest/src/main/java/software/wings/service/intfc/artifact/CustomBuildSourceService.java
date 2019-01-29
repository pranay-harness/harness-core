package software.wings.service.intfc.artifact;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

public interface CustomBuildSourceService {
  List<BuildDetails> getBuilds(@NotEmpty String appId, @NotEmpty String artifactStreamId);
}
