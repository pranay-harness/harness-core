package io.harness.migrations.all;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.yaml.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.EntityType;
import software.wings.beans.EntityYamlRecord;
import software.wings.beans.Event;
import software.wings.beans.security.UserGroup;
import software.wings.service.impl.EntityHelper;
import software.wings.service.intfc.AuditService;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;

@Slf4j
@Singleton
@OwnedBy(PL)
public class UserGroupAuditDiffMigration implements Migration {
    @Inject private HPersistence persistence;
    @Inject private AuditService auditService;
    @Inject private EntityHelper entityHelper;

    @Override
    public void migrate() {
        try (HIterator<UserGroup> userGroups = new HIterator<>(persistence.createQuery(UserGroup.class).fetch())) {
            while (userGroups.hasNext()) {
                UserGroup userGroup = userGroups.next();
                log.info(" Starting Migration For Adding user group yaml content for account {} and user group {}",
                        userGroup.getAccountId(), userGroup.getUuid());
                EntityAuditRecord.EntityAuditRecordBuilder builder = EntityAuditRecord.builder();
                loadMetaDataForEntity(userGroup, builder);

                EntityAuditRecord record = builder.build();
                UserGroup userGroupAudit = userGroup.buildUserGroupAudit();
                String yamlContent = toYamlString(userGroupAudit);
                String yamlPath = entityHelper.getFullYamlPathForEntity(userGroup, record);

                String entityId = userGroup.getUuid();
                String entityType = EntityType.USER_GROUP.name();
                EntityYamlRecord yamlRecord = EntityYamlRecord.builder()
                        .uuid(generateUuid())
                        .accountId(userGroup.getAccountId())
                        .createdAt(currentTimeMillis())
                        .entityId(entityId)
                        .entityType(entityType)
                        .yamlPath(yamlPath)
                        .yamlSha(sha1Hex(yamlContent))
                        .yamlContent(yamlContent)
                        .build();
                log.info("Yaml record for user group {} is yamlrecord {}", userGroup.getName(), yamlRecord);
                persistence.save(yamlRecord);
            }
        } catch (Exception ex) {
            log.error(String.join("Exception while Adding user group yaml content"));
        }
    }

    public String toYamlString(Object object) {
        String yamlString;
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            yamlString = YamlUtils.cleanupYaml(mapper.writeValueAsString(object));
        } catch (Exception ex) {
            throw new InvalidRequestException("Encountered exception while serializing user group " + ex.getMessage());
        }
        return yamlString;
    }

    private void loadMetaDataForEntity(UserGroup userGroup, EntityAuditRecord.EntityAuditRecordBuilder builder){
        builder.entityType(EntityType.USER_GROUP.name()).entityName(userGroup.getName())
                .entityId(userGroup.getUuid()).affectedResourceId(userGroup.getUuid())
                .affectedResourceName(userGroup.getName()).affectedResourceType(EntityType.USER_GROUP.name()).operationType(Event.Type.CREATE.name());
        log.info("Migrating user group. User Group {} account {}", userGroup.getUuid(), userGroup.getAccountId());
    }
}
