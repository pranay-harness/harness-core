/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO.ApiCallLogDTOField;
import io.harness.cvng.beans.cvnglog.CVNGLogDTO;
import io.harness.cvng.beans.cvnglog.CVNGLogType;
import io.harness.cvng.beans.cvnglog.TraceableType;
import io.harness.cvng.core.entities.CVNGLog;
import io.harness.cvng.core.entities.CVNGLog.CVNGLogKeys;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.CVNGLogService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CV)
public class CVNGLogServiceImplTest extends CvNextGenTestBase {
  @Inject private HPersistence hPersistence;
  @Inject private CVNGLogService cvngLogService;
  @Mock private VerificationTaskService verificationTaskService;

  private String accountId;
  private String traceableId;
  private long createdAt;
  private Instant requestTime;
  private Instant responseTime;
  private Instant startTime;
  private Instant endTime;

  @Before
  public void setup() throws IllegalAccessException {
    accountId = generateUuid();
    traceableId = generateUuid();
    requestTime = Instant.now();
    responseTime = Instant.now();
    startTime = Instant.now().minusSeconds(5);
    endTime = Instant.now();
    createdAt = Instant.now().toEpochMilli();
    FieldUtils.writeField(cvngLogService, "verificationTaskService", verificationTaskService, true);
    when(verificationTaskService.get(any())).thenReturn(null);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testSave_verificationApiCallLogMultipleTraceableId() {
    List<CVNGLogDTO> cvngLogRecordsDTO =
        IntStream.range(0, 7).mapToObj(index -> createApiCallLogDTOVerification()).collect(Collectors.toList());
    traceableId = generateUuid();
    cvngLogRecordsDTO.addAll(
        IntStream.range(0, 3).mapToObj(index -> createApiCallLogDTOVerification()).collect(Collectors.toList()));
    cvngLogService.save(cvngLogRecordsDTO);
    List<CVNGLog> cvngLogs = hPersistence.createQuery(CVNGLog.class).filter(CVNGLogKeys.accountId, accountId).asList();
    assertThat(cvngLogs).hasSize(2);
    assertThat(cvngLogs.get(0).getLogRecords()).hasSize(7);
    assertThat(cvngLogs.get(1).getLogRecords()).hasSize(3);
    cvngLogs.forEach(logRecord -> {
      assertThat(logRecord.getAccountId()).isEqualTo(accountId);
      assertThat(logRecord.getStartTime()).isEqualTo(startTime);
      assertThat(logRecord.getEndTime()).isEqualTo(endTime);
      assertThat(logRecord.getTraceableType()).isEqualTo(TraceableType.VERIFICATION_TASK);
    });
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testSave_onboardingApiCallLog() {
    List<CVNGLogDTO> cvngLogRecordsDTO =
        IntStream.range(0, 7).mapToObj(index -> createApiCallLogDTOOnboarding()).collect(Collectors.toList());
    cvngLogService.save(cvngLogRecordsDTO);
    List<CVNGLog> cvngLogs = hPersistence.createQuery(CVNGLog.class).filter(CVNGLogKeys.accountId, accountId).asList();
    assertThat(cvngLogs).hasSize(1);
    assertThat(cvngLogs.get(0).getLogRecords()).hasSize(7);
    cvngLogs.forEach(logRecord -> {
      assertThat(logRecord.getAccountId()).isEqualTo(accountId);
      assertThat(logRecord.getTraceableId()).isEqualTo(traceableId);
      assertThat(logRecord.getStartTime()).isEqualTo(startTime);
      assertThat(logRecord.getEndTime()).isEqualTo(endTime);
      assertThat(logRecord.getTraceableType()).isEqualTo(TraceableType.ONBOARDING);
    });
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testSave_addsAllFields() {
    List<CVNGLogDTO> cvngLogRecordsDTO =
        IntStream.range(0, 7).mapToObj(index -> createApiCallLogDTOOnboarding()).collect(Collectors.toList());
    cvngLogService.save(cvngLogRecordsDTO);
    List<CVNGLog> cvngLogs = hPersistence.createQuery(CVNGLog.class).filter(CVNGLogKeys.accountId, accountId).asList();
    cvngLogs.forEach(logRecord -> {
      List<Field> fields = ReflectionUtils.getAllDeclaredAndInheritedFields(CVNGLog.class);
      fields.forEach(field -> {
        try {
          field.setAccessible(true);
          assertThat(field.get(logRecord)).withFailMessage("field %s is null", field.getName()).isNotNull();
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      });
    });
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetOnboardingLogs() {
    List<CVNGLogDTO> cvngLogRecordsDTO =
        IntStream.range(0, 7).mapToObj(index -> createApiCallLogDTOOnboarding()).collect(Collectors.toList());
    cvngLogService.save(cvngLogRecordsDTO);
    PageResponse<CVNGLogDTO> cvngLogs =
        cvngLogService.getOnboardingLogs(accountId, traceableId, CVNGLogType.API_CALL_LOG, 0, 7);
    assertThat(cvngLogs.getContent()).hasSize(7);
    final int[] timeCounter = {0};
    final Long[] lastCreatedAt = {Long.MAX_VALUE};
    cvngLogs.getContent().sort(Comparator.comparing(log -> ((ApiCallLogDTO) log).getRequestTime()).reversed());
    cvngLogs.getContent().forEach(logRecord -> {
      assertThat(logRecord.getAccountId()).isEqualTo(accountId);
      assertThat(logRecord.getTraceableId()).isEqualTo(traceableId);
      assertThat(((ApiCallLogDTO) logRecord).getResponseTime())
          .isEqualTo(responseTime.minusSeconds(timeCounter[0]).toEpochMilli());
      assertThat(((ApiCallLogDTO) logRecord).getRequestTime())
          .isEqualTo(requestTime.minusSeconds(timeCounter[0]).toEpochMilli());
      assertThat(logRecord.getStartTime()).isEqualTo(startTime.toEpochMilli());
      assertThat(logRecord.getEndTime()).isEqualTo(endTime.toEpochMilli());
      assertThat(logRecord.getTraceableType()).isEqualTo(TraceableType.ONBOARDING);
      assertThat(logRecord.getType()).isEqualTo(CVNGLogType.API_CALL_LOG);
      assertThat(logRecord.getCreatedAt()).isLessThanOrEqualTo(lastCreatedAt[0]);
      lastCreatedAt[0] = logRecord.getCreatedAt();
      timeCounter[0] += 10;
    });
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetCVNGLogs() throws IllegalAccessException {
    List<CVNGLogDTO> cvngLogRecordsDTO =
        IntStream.range(0, 3).mapToObj(index -> createApiCallLogDTOVerification()).collect(Collectors.toList());
    cvngLogService.save(cvngLogRecordsDTO);
    List<String> traceableIds =
        cvngLogRecordsDTO.stream().map(logRecord -> logRecord.getTraceableId()).collect(Collectors.toList());
    VerificationTaskService verificationTaskService = mock(VerificationTaskServiceImpl.class);
    CVConfigService cvConfigService = mock(CVConfigService.class);
    FieldUtils.writeField(cvngLogService, "verificationTaskService", verificationTaskService, true);
    FieldUtils.writeField(cvngLogService, "cvConfigService", cvConfigService, true);
    when(verificationTaskService.getServiceGuardVerificationTaskIds(any(), any(List.class))).thenReturn(traceableIds);
    PageResponse<CVNGLogDTO> cvngLogs =
        cvngLogService.getCVNGLogs(accountId, "", "", null, null, startTime.minus(Duration.ofMillis(1)),
            endTime.plus(Duration.ofMillis(1)), null, CVNGLogType.API_CALL_LOG, 0, 3);
    assertThat(cvngLogs.getContent()).hasSize(3);
    final int[] timeCounter = {0};
    cvngLogs.getContent().sort(Comparator.comparing(log -> ((ApiCallLogDTO) log).getRequestTime()).reversed());
    (cvngLogs.getContent()).forEach(logRecord -> {
      assertThat(logRecord.getAccountId()).isEqualTo(accountId);
      assertThat(logRecord.getTraceableId()).isEqualTo(traceableId);
      assertThat(((ApiCallLogDTO) logRecord).getRequestTime())
          .isEqualTo(requestTime.minusSeconds(timeCounter[0]).toEpochMilli());
      assertThat(((ApiCallLogDTO) logRecord).getResponseTime())
          .isEqualTo(responseTime.minusSeconds(timeCounter[0]).toEpochMilli());
      assertThat(logRecord.getStartTime()).isEqualTo(startTime.toEpochMilli());
      assertThat(logRecord.getEndTime()).isEqualTo(endTime.toEpochMilli());
      assertThat(logRecord.getTraceableType()).isEqualTo(TraceableType.VERIFICATION_TASK);
      timeCounter[0] += 10;
    });
  }

  private CVNGLogDTO createApiCallLogDTOVerification() {
    requestTime = requestTime.plusSeconds(10);
    responseTime = responseTime.plusSeconds(10);
    return ApiCallLogDTO.builder()
        .accountId(accountId)
        .traceableId(traceableId)
        .requestTime(requestTime.toEpochMilli())
        .requests(Arrays.asList(
            ApiCallLogDTOField.builder().name("url").value("http:/appd.com").type(ApiCallLogDTO.FieldType.URL).build()))
        .responses(Arrays.asList(
            ApiCallLogDTOField.builder().name("Status Code").value("200").type(ApiCallLogDTO.FieldType.NUMBER).build(),
            ApiCallLogDTOField.builder()
                .name("response body")
                .value("success")
                .type(ApiCallLogDTO.FieldType.JSON)
                .build()))
        .responseTime(responseTime.toEpochMilli())
        .startTime(startTime.toEpochMilli())
        .endTime(endTime.toEpochMilli())
        .createdAt(createdAt)
        .traceableType(TraceableType.VERIFICATION_TASK)
        .build();
  }

  private CVNGLogDTO createApiCallLogDTOOnboarding() {
    requestTime = requestTime.plusSeconds(10);
    responseTime = responseTime.plusSeconds(10);
    return ApiCallLogDTO.builder()
        .accountId(accountId)
        .traceableId(traceableId)
        .requestTime(requestTime.toEpochMilli())
        .responseTime(responseTime.toEpochMilli())
        .requests(Arrays.asList(
            ApiCallLogDTOField.builder().name("url").value("http:/appd.com").type(ApiCallLogDTO.FieldType.URL).build()))
        .responses(Arrays.asList(
            ApiCallLogDTOField.builder().name("Status Code").value("200").type(ApiCallLogDTO.FieldType.NUMBER).build(),
            ApiCallLogDTOField.builder()
                .name("response body")
                .value("success")
                .type(ApiCallLogDTO.FieldType.JSON)
                .build()))
        .startTime(startTime.toEpochMilli())
        .endTime(endTime.toEpochMilli())
        .createdAt(createdAt)
        .traceableType(TraceableType.ONBOARDING)
        .build();
  }
}
