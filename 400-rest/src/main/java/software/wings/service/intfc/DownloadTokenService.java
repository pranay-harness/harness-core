package software.wings.service.intfc;

/**
 * Created by peeyushaggarwal on 12/13/16.
 */
public interface DownloadTokenService {
  String createDownloadToken(String resource);
  void validateDownloadToken(String resource, String token);
}
