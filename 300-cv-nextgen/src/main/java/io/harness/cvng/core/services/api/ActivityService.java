package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.ActivityDTO;

public interface ActivityService { void register(String accountId, String webhookToken, ActivityDTO activityDTO); }
