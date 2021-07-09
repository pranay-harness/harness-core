package io.harness.migrations.all;

import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;

import static io.harness.annotations.dev.HarnessTeam.CDC;


@Slf4j
@Singleton
@OwnedBy(CDC)
public class DeleteInvalidArtifactStreams implements Migration {

    @Override
    public void migrate() {


    }


}
