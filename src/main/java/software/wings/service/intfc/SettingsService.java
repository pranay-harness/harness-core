package software.wings.service.intfc;

import software.wings.beans.SettingAttribute;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.util.List;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by anubhaw on 5/17/16.
 */
public interface SettingsService {
  PageResponse<SettingAttribute> list(PageRequest<SettingAttribute> req);

  SettingAttribute save(SettingAttribute envVar);

  SettingAttribute get(String appId, String varId);

  SettingAttribute update(SettingAttribute envVar);

  void delete(String appId, String varId);

  SettingAttribute getByName(String appId, String attributeName);

  void createDefaultSettings(String appId);

  List<SettingAttribute> getConnectionAttributes(MultivaluedMap<String, String> queryParameters);
  List<SettingAttribute> getBastionHostAttributes(MultivaluedMap<String, String> queryParameters);
}
