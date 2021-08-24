package software.wings.service.intfc;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

/**
 * Created by peeyushaggarwal on 12/13/16.
 */
@TargetModule(HarnessModule._980_COMMONS)
public interface DownloadTokenService {
  String createDownloadToken(String resource);
  void validateDownloadToken(String resource, String token);
}
