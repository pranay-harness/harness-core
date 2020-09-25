package io.harness.ng.core.activityhistory.entity;

import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Value
@SuperBuilder
@NoArgsConstructor
@Entity(value = "ngActivity", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.ng.core.activity.ConnectivityCheckDetail")
public class ConnectivityCheckDetail extends NGActivity {}