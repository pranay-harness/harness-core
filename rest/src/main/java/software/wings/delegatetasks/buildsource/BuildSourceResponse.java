package software.wings.delegatetasks.buildsource;

import lombok.Builder;
import lombok.Data;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

/**
 * Created by anubhaw on 7/20/18.
 */
@Data
@Builder
public class BuildSourceResponse {
  private List<BuildDetails> buildDetails;
}
