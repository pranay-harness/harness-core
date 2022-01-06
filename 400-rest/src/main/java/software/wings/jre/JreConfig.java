/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.jre;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Singleton
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@OwnedBy(DEL)
public class JreConfig {
  String version;
  String jreDirectory;
  String jreMacDirectory;
  String jreTarPath;
  String alpnJarPath;
}
