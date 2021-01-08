package timescaledb

import (
	"context"
	"fmt"
	"regexp"
	"strconv"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/db"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.uber.org/zap"
	"gopkg.in/DATA-DOG/go-sqlmock.v1"
)

func Test_SingleWrite(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	table := "tests"
	account := "account"
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	stage := "stage"
	step := "step"
	report := "junit"

	tn := time.Now()

	oldNow := now
	defer func() { now = oldNow }()
	now = func() time.Time { return tn }

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}
	valueStrings := constructPsqlInsertStmt(1, 19)
	stmt := fmt.Sprintf(
		`
				INSERT INTO %s
				(time, account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id, report, name, suite_name,
				class_name, duration_ms, status, message, type, description, stdout, stderr)
				VALUES %s`, table, valueStrings)
	stmt = regexp.QuoteMeta(stmt)
	mock.ExpectExec(stmt).
		WithArgs(tn, account, org, project,
			pipeline, build, stage, step, report, "blah", "", "", 0, "", "", "", "", "", "").WillReturnResult(sqlmock.NewResult(0, 1))
	tdb := &TimeScaleDb{Conn: db, Log: log}
	test1 := &types.TestCase{Name: "blah"}
	err = tdb.Write(ctx, table, account, org, project, pipeline, build, stage, step, report, test1)
	assert.Nil(t, err, nil)
}

func Test_Summary(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	table := "tests"
	account := "account"
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	report := "junit"

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}
	col := []string{"duration_ms", "status", "name"}
	rows := sqlmock.NewRows(col).
		AddRow(10, "failed", "t1").
		AddRow(25, "passed", "t2")
	query := fmt.Sprintf(`
		SELECT duration_ms, status, name FROM %s WHERE account_id = $1
		AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND report = $6;`, table)
	t1 := types.TestSummary{Name: "t1", Status: types.StatusFailed}
	t2 := types.TestSummary{Name: "t2", Status: types.StatusPassed}
	summary := []types.TestSummary{t1, t2}
	exp := types.SummaryResponse{
		TotalTests: 2,
		TimeMs:     35,
		Tests:      summary,
	}
	query = regexp.QuoteMeta(query)
	mock.ExpectQuery(query).
		WithArgs(account, org, project, pipeline, build, report).WillReturnRows(rows)
	tdb := &TimeScaleDb{Conn: db, Log: log}
	got, err := tdb.Summary(ctx, table, account, org, project, pipeline, build, report)
	assert.Nil(t, err, nil)
	assert.Equal(t, got.TotalTests, exp.TotalTests)
	assert.Equal(t, got.TimeMs, exp.TimeMs)
	assert.ElementsMatch(t, got.Tests, exp.Tests)
}

func Test_GetTestCases(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	table := "tests"
	account := "account"
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	report := "junit"
	suite := "s1"

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}
	col := []string{"name", "suite_name", "class_name", "duration_ms", "status", "message", "description", "type", "stdout",
		"stderr", "full_count"}
	rows := sqlmock.NewRows(col).
		AddRow("t1", suite, "c1", 10, "failed", "m1", "d1", "type1", "o1", "e1", 2).
		AddRow("t2", suite, "c2", 5, "error", "m2", "d2", "type2", "o2", "e2", 2)
	tc1 := types.TestCase{
		Name:      "t1",
		ClassName: "c1",
		SuiteName: suite,
		Result: types.Result{
			Status:  types.StatusFailed,
			Message: "m1",
			Type:    "type1",
			Desc:    "d1",
		},
		DurationMs: 10,
		SystemOut:  "o1",
		SystemErr:  "e1",
	}
	tc2 := types.TestCase{
		Name:      "t2",
		ClassName: "c2",
		SuiteName: suite,
		Result: types.Result{
			Status:  types.StatusError,
			Message: "m2",
			Type:    "type2",
			Desc:    "d2",
		},
		DurationMs: 5,
		SystemOut:  "o2",
		SystemErr:  "e2",
	}
	tests := []types.TestCase{tc1, tc2}
	query := fmt.Sprintf(
		`
		SELECT name, suite_name, class_name, duration_ms, status, message,
		description, type, stdout, stderr, COUNT(*) OVER() AS full_count
		FROM %s
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND report = $6 AND suite_name = $7 AND status IN (%s)
		ORDER BY %s %s, %s %s
		LIMIT $8 OFFSET $9;`, table, "'failed', 'error'", "duration_ms", "DESC", "name", "ASC")
	query = regexp.QuoteMeta(query)
	mock.ExpectQuery(query).
		WithArgs(account, org, project, pipeline, build, report, suite, defaultLimit, defaultOffset).WillReturnRows(rows)
	tdb := &TimeScaleDb{Conn: db, Log: log}
	got, err := tdb.GetTestCases(ctx, table, account, org, project, pipeline, build, report, suite, "duration_ms", "failed", desc, "", "")
	li, _ := strconv.Atoi(defaultLimit)
	assert.Nil(t, err, nil)
	assert.Equal(t, got.Metadata.TotalPages, 1)
	assert.Equal(t, got.Metadata.TotalItems, 2)
	assert.Equal(t, got.Metadata.PageItemCount, 2)
	assert.Equal(t, got.Metadata.PageSize, li)
	assert.ElementsMatch(t, got.Tests, tests)
}

func Test_GetTestSuites(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	table := "tests"
	account := "account"
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	report := "junit"

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}
	col := []string{"suite_name", "duration_ms", "total_tests", "skipped_tests", "passed_tests", "failed_tests", "fail_pct", "full_count"}
	rows := sqlmock.NewRows(col).
		AddRow("s1", 10, 4, 1, 1, 2, 50, 2).
		AddRow("s2", 5, 2, 1, 1, 0, 0, 2)
	ts1 := types.TestSuite{
		Name:         "s1",
		DurationMs:   10,
		TotalTests:   4,
		FailedTests:  2,
		SkippedTests: 1,
		PassedTests:  1,
		FailPct:      50,
	}
	ts2 := types.TestSuite{
		Name:         "s2",
		DurationMs:   5,
		TotalTests:   2,
		FailedTests:  0,
		SkippedTests: 1,
		PassedTests:  1,
		FailPct:      0,
	}
	suites := []types.TestSuite{ts1, ts2}
	query := fmt.Sprintf(
		`
		SELECT suite_name, SUM(duration_ms) AS duration_ms, COUNT(*) AS total_tests,
		SUM(CASE WHEN status = 'skipped' THEN 1 ELSE 0 END) AS skipped_tests,
		SUM(CASE WHEN status = 'passed' THEN 1 ELSE 0 END) AS passed_tests,
		SUM(CASE WHEN status = 'failed' OR status = 'error' THEN 1 ELSE 0 END) AS failed_tests,
		SUM(CASE WHEN status = 'failed' OR status = 'error' THEN 1 ELSE 0 END) * 100 / COUNT(*) AS fail_pct,
		COUNT(*) OVER() AS full_count
		FROM %s
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND report = $6 AND status IN (%s)
		GROUP BY suite_name
		ORDER BY %s %s, %s %s
		LIMIT $7 OFFSET $8;`, table, "'failed', 'error', 'passed', 'skipped'", "fail_pct", desc, "suite_name", asc)
	query = regexp.QuoteMeta(query)
	mock.ExpectQuery(query).
		WithArgs(account, org, project, pipeline, build, report, defaultLimit, defaultOffset).WillReturnRows(rows)
	tdb := &TimeScaleDb{Conn: db, Log: log}
	got, err := tdb.GetTestSuites(ctx, table, account, org, project, pipeline, build, report, "", "", "", "", "")
	li, _ := strconv.Atoi(defaultLimit)
	assert.Nil(t, err, nil)
	assert.Equal(t, got.Metadata.TotalPages, 1)
	assert.Equal(t, got.Metadata.TotalItems, 2)
	assert.Equal(t, got.Metadata.PageItemCount, 2)
	assert.Equal(t, got.Metadata.PageSize, li)
	assert.ElementsMatch(t, got.Suites, suites)
}
