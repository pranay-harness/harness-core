package io.harness.ng.core.activityhistory.entity;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Value
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "entityActivity", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.ng.core.activity.ConnectivityCheckDetail")
public class ConnectivityCheckDetail extends NGActivity {}
