package io.harness.ngtriggers.mapper;

import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.config.HeaderConfig;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.NGTriggerResponseDTO;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.metadata.NGTriggerMetadata;
import io.harness.ngtriggers.beans.entity.metadata.WebhookMetadata;
import io.harness.ngtriggers.beans.source.NGTriggerSource;
import io.harness.ngtriggers.beans.source.NGTriggerType;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.yaml.utils.YamlPipelineUtils;

import java.io.IOException;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGTriggerElementMapper {
  public NGTriggerConfig toTriggerConfig(String yaml) {
    try {
      return YamlPipelineUtils.read(yaml, NGTriggerConfig.class);
    } catch (IOException e) {
      throw new InvalidRequestException(e.getMessage()); // update this message
    }
  }

  public NGTriggerEntity toTriggerEntity(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String yaml) {
    NGTriggerConfig config = toTriggerConfig(yaml);
    return toTriggerEntity(accountIdentifier, orgIdentifier, projectIdentifier, config, yaml);
  }

  public NGTriggerEntity toTriggerEntity(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, String yaml) {
    NGTriggerConfig config = toTriggerConfig(yaml);
    if (!identifier.equals(config.getIdentifier())) {
      throw new InvalidRequestException("Identifier in url and yaml do not match");
    }
    return toTriggerEntity(accountIdentifier, orgIdentifier, projectIdentifier, config, yaml);
  }

  public NGTriggerEntity toTriggerEntity(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, NGTriggerConfig config, String yaml) {
    return NGTriggerEntity.builder()
        .name(config.getName())
        .identifier(config.getIdentifier())
        .description(config.getDescription())
        .yaml(yaml)
        .type(config.getSource().getType())
        .accountId(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .targetIdentifier(config.getTarget().getTargetIdentifier())
        .targetType(config.getTarget().getType())
        .metadata(toMetadata(config.getSource()))
        .build();
  }

  private NGTriggerMetadata toMetadata(NGTriggerSource triggerSource) {
    NGTriggerType type = triggerSource.getType();
    if (type == NGTriggerType.WEBHOOK) {
      WebhookTriggerConfig webhookTriggerConfig = (WebhookTriggerConfig) triggerSource.getSpec();
      WebhookMetadata metadata = WebhookMetadata.builder()
                                     .type(webhookTriggerConfig.getType())
                                     .repoURL(webhookTriggerConfig.getSpec().getRepoUrl())
                                     .build();
      return NGTriggerMetadata.builder().webhook(metadata).build();
    }
    throw new InvalidRequestException("Type " + type.toString() + " is invalid");
  }

  public NGTriggerResponseDTO toResponseDTO(NGTriggerEntity ngTriggerEntity) {
    return NGTriggerResponseDTO.builder()
        .name(ngTriggerEntity.getName())
        .identifier(ngTriggerEntity.getIdentifier())
        .description(ngTriggerEntity.getDescription())
        .type(ngTriggerEntity.getType())
        .accountIdentifier(ngTriggerEntity.getAccountId())
        .orgIdentifier(ngTriggerEntity.getOrgIdentifier())
        .projectIdentifier(ngTriggerEntity.getProjectIdentifier())
        .targetIdentifier(ngTriggerEntity.getTargetIdentifier())
        .version(ngTriggerEntity.getVersion())
        .yaml(ngTriggerEntity.getYaml())
        .build();
  }

  public TriggerWebhookEvent toNGTriggerWebhookEvent(
      String accountIdentifier, String payload, List<HeaderConfig> headerConfigs) {
    return TriggerWebhookEvent.builder().accountId(accountIdentifier).headers(headerConfigs).payload(payload).build();
  }
}
