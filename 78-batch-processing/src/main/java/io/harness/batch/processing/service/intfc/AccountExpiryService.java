package io.harness.batch.processing.service.intfc;

import software.wings.beans.Account;

public interface AccountExpiryService { boolean dataPipelineCleanup(Account account); }
