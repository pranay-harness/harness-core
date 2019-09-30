package io.harness.event.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;
import io.harness.queue.Queuable.Keys;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "genericEvent", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Indexes({
  @Index(options = @IndexOptions(name = "version_1_earliestGet_1_resetTimestamp_1", background = true),
      fields = { @Field(Keys.version)
                 , @Field(Keys.earliestGet), @Field(Keys.resetTimestamp) })
  ,
      @Index(options = @IndexOptions(name = "version_1_running_1_earliestGet_1", background = true), fields = {
        @Field(Keys.version), @Field(Keys.running), @Field(Keys.earliestGet)
      })
})
public class QueableEvent extends Queuable {
  private Event event;
}
