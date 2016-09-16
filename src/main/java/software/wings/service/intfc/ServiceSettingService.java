package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ServiceSetting;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 9/14/16.
 */
public interface ServiceSettingService {
  PageResponse<ServiceSetting> list(PageRequest<ServiceSetting> request);

  @ValidationGroups(Create.class) String save(@Valid ServiceSetting configFile);

  ServiceSetting get(@NotEmpty String appId, @NotEmpty String settingId);

  @ValidationGroups(Update.class) ServiceSetting update(@Valid ServiceSetting serviceSetting);

  void delete(@NotEmpty String appId, @NotEmpty String settingId);

  List<ServiceSetting> getSettingsForEntity(String appId, String templateId, String entityId);

  List<ServiceSetting> getServiceSettingByTemplate(String appId, String envId, ServiceTemplate serviceTemplate);

  void deleteByEntityId(String appId, String templateId, String entityId);

  void deleteByTemplateId(String appId, String serviceTemplateId);

  void deleteByEntityId(String appId, String entityId);
}
