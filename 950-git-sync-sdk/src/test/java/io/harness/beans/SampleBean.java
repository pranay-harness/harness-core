package io.harness.beans;

import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Entity(value = "sampleBean", noClassnameStored = true)
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("sampleBean")
public class SampleBean implements UuidAware, PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  String test1;
}
