package io.harness.repositories;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@FieldNameConstants(innerTypeName = "DocumentOneKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Document("DocumentOne")
@TypeAlias("io.harness.cvng.core.entities.DocumentOne")
@SuperBuilder
public class DocumentOne {
  @JsonIgnore @Id String id;

  private String name;
  private String address;
  private long amount;
}
