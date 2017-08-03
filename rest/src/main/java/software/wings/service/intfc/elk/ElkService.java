package software.wings.service.intfc.elk;

import software.wings.beans.SettingAttribute;

import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 08/01/17.
 */
public interface ElkService { void validateConfig(@NotNull SettingAttribute settingAttribute); }
