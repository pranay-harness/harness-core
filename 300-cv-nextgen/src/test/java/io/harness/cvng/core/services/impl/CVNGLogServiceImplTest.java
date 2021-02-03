package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO;
import io.harness.cvng.beans.cvnglog.CVNGLogDTO;
import io.harness.cvng.beans.cvnglog.TraceableType;
import io.harness.cvng.core.entities.CVNGLog;
import io.harness.cvng.core.entities.CVNGLog.CVNGLogKeys;
import io.harness.cvng.core.services.api.CVNGLogService;
import io.harness.persistence.HPersistence;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CVNGLogServiceImplTest extends CvNextGenTest {
  @Inject private HPersistence hPersistence;
  @Inject private CVNGLogService cvngLogService;

  private String accountId;
  private String traceableId;
  private long createdAt;
  private Instant requestTime;
  private Instant responseTime;
  private Instant startTime;
  private Instant endTime;

  @Before
  public void setup() {
    accountId = generateUuid();
    traceableId = generateUuid();
    requestTime = Instant.now();
    responseTime = Instant.now();
    startTime = Instant.now();
    endTime = Instant.now();
    createdAt = Instant.now().toEpochMilli();
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

  private CVNGLogDTO createApiCallLogDTOVerification() {
    requestTime = requestTime.plusSeconds(10);
    responseTime = responseTime.plusSeconds(10);
    return ApiCallLogDTO.builder()
        .accountId(accountId)
        .traceableId(traceableId)
        .requestTime(requestTime)
        .responseTime(responseTime)
        .startTime(startTime)
        .endTime(endTime)
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
        .requestTime(requestTime)
        .responseTime(responseTime)
        .startTime(startTime)
        .endTime(endTime)
        .createdAt(createdAt)
        .traceableType(TraceableType.ONBOARDING)
        .build();
  }
}
