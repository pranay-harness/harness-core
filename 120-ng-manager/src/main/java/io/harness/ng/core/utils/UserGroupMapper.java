package io.harness.ng.core.utils;

import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ng.core.mapper.TagMapper.convertToMap;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import io.harness.ng.core.dto.NotificationSettingConfigDTO;
import io.harness.ng.core.dto.PagerDutyConfigDTO;
import io.harness.ng.core.dto.SlackConfigDTO;
import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.ng.core.entities.NotificationSettingConfig;
import io.harness.ng.core.entities.PagerDutyConfig;
import io.harness.ng.core.entities.SlackConfig;
import io.harness.ng.core.entities.UserGroup;

import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UserGroupMapper {
  public static UserGroupDTO toDTO(UserGroup userGroup) {
    return (userGroup == null)
        ? null
        : UserGroupDTO.builder()
              .accountIdentifier(userGroup.getAccountIdentifier())
              .orgIdentifier(userGroup.getOrgIdentifier())
              .projectIdentifier(userGroup.getProjectIdentifier())
              .identifier(userGroup.getIdentifier())
              .description(userGroup.getDescription())
              .tags(convertToMap(userGroup.getTags()))
              .name(userGroup.getName())
              .notificationConfigs(
                  userGroup.getNotificationConfigs().stream().map(UserGroupMapper::toDTO).collect(Collectors.toList()))
              .users(userGroup.getUsers())
              .lastModifiedAt(userGroup.getLastModifiedAt())
              .build();
  }

  public static UserGroup toEntity(UserGroupDTO userGroupDTO) {
    return (userGroupDTO == null)
        ? null
        : UserGroup.builder()
              .accountIdentifier(userGroupDTO.getAccountIdentifier())
              .orgIdentifier(userGroupDTO.getOrgIdentifier())
              .projectIdentifier(userGroupDTO.getProjectIdentifier())
              .identifier(userGroupDTO.getIdentifier())
              .name(userGroupDTO.getName())
              .notificationConfigs((Optional.ofNullable(userGroupDTO.getNotificationConfigs()).orElse(emptyList()))
                                       .stream()
                                       .map(UserGroupMapper::toEntity)
                                       .collect(Collectors.toList()))
              .description(Optional.ofNullable(userGroupDTO.getDescription()).orElse(""))
              .tags(convertToList(Optional.ofNullable(userGroupDTO.getTags()).orElse(emptyMap())))
              .users(Optional.ofNullable(userGroupDTO.getUsers()).orElse(emptyList()))
              .build();
  }

  public static NotificationSettingConfig toEntity(NotificationSettingConfigDTO dto) {
    if (dto == null) {
      return null;
    }
    switch (dto.getType()) {
      case Slack:
        return SlackConfig.builder().slackId(((SlackConfigDTO) dto).getSlackId()).build();
      case PagerDuty:
        return PagerDutyConfig.builder().pagerDutyId(((PagerDutyConfigDTO) dto).getPagerDutyId()).build();
      default:
        throw new IllegalArgumentException("This is not a valid Notification Setting Type: " + dto.getType());
    }
  }

  public static NotificationSettingConfigDTO toDTO(NotificationSettingConfig entity) {
    if (entity == null) {
      return null;
    }
    switch (entity.getType()) {
      case Slack:
        return SlackConfigDTO.builder().slackId(((SlackConfig) entity).getSlackId()).build();
      case PagerDuty:
        return PagerDutyConfigDTO.builder().pagerDutyId(((PagerDutyConfig) entity).getPagerDutyId()).build();
      default:
        throw new IllegalArgumentException("This is not a valid Notification Setting Type: " + entity.getType());
    }
  }
}
