package io.harness.accesscontrol.acl.models;

import io.harness.beans.EmbeddedUser;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document("acl")
public abstract class ACL {
  @Id @org.mongodb.morphia.annotations.Id private String id;
  @CreatedDate Long createdAt;
  @Version Long version;
  @LastModifiedDate Long lastModifiedAt;
  @CreatedBy EmbeddedUser createdBy;
  @LastModifiedBy EmbeddedUser lastModifiedBy;
}
