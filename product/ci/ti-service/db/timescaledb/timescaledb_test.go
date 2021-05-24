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
	repo := "repo"
	sha := "sha"

	tn := time.Now()

	oldNow := now
	defer func() { now = oldNow }()
	now = func() time.Time { return tn }

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}
	valueStrings := constructPsqlInsertStmt(1, 21)
	stmt := fmt.Sprintf(
		`
				INSERT INTO %s
				(created_at, account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id, report, repo, commit_id,
				name, suite_name, class_name, duration_ms, result, message, type, description, stdout, stderr)
				VALUES %s`, table, valueStrings)
	stmt = regexp.QuoteMeta(stmt)
	mock.ExpectExec(stmt).
		WithArgs(tn, account, org, project,
			pipeline, build, stage, step, report, repo, sha, "blah", "", "", 0, "", "", "", "", "", "").WillReturnResult(sqlmock.NewResult(0, 1))
	tdb := &TimeScaleDb{Conn: db, Log: log, EvalTable: table}
	test1 := &types.TestCase{Name: "blah"}
	err = tdb.Write(ctx, account, org, project, pipeline, build, stage, step, report, repo, sha, test1)
	assert.Nil(t, err, nil)
}

func Test_Write_Batch(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	// Set write batch size of 2 and try to write 3 tests.
	writeBatchSize = 2

	table := "tests"
	account := "account"
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	stage := "stage"
	step := "step"
	report := "junit"
	repo := "repo"
	sha := "sha"

	tn := time.Now()

	oldNow := now
	defer func() { now = oldNow }()
	now = func() time.Time { return tn }

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}
	valueStrings := constructPsqlInsertStmt(1, 21)
	stmt := fmt.Sprintf(
		`
				INSERT INTO %s
				(created_at, account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id, report, repo, commit_id,
				name, suite_name, class_name, duration_ms, result, message, type, description, stdout, stderr)
				VALUES %s`, table, valueStrings)
	stmt = regexp.QuoteMeta(stmt)
	// First batched statement
	mock.ExpectExec(stmt).
		WithArgs(tn, account, org, project, pipeline, build, stage, step, report, repo, sha, "test1", "suite1", "class1", 10, "passed", "msg1", "type1", "desc1", "out1", "err1",
			tn, account, org, project, pipeline, build, stage, step, report, repo, sha, "test2", "suite2", "class2", 11, "failed", "msg2", "type2", "desc2", "out2", "err2").WillReturnResult(sqlmock.NewResult(0, 2))
	// Leftover writes
	mock.ExpectExec(stmt).
		WithArgs(tn, account, org, project, pipeline, build, stage, step, report, repo, sha, "test3", "suite3", "class3", 12, "error", "msg3", "type3", "desc3", "out3", "err3").WillReturnResult(sqlmock.NewResult(0, 1))
	test1 := &types.TestCase{Name: "test1", ClassName: "class1", SuiteName: "suite1",
		SystemOut: "out1", SystemErr: "err1", DurationMs: 10,
		Result: types.Result{Status: types.StatusPassed, Message: "msg1", Type: "type1", Desc: "desc1"}}
	test2 := &types.TestCase{Name: "test2", ClassName: "class2", SuiteName: "suite2",
		SystemOut: "out2", SystemErr: "err2", DurationMs: 11,
		Result: types.Result{Status: types.StatusFailed, Message: "msg2", Type: "type2", Desc: "desc2"}}
	test3 := &types.TestCase{Name: "test3", ClassName: "class3", SuiteName: "suite3",
		SystemOut: "out3", SystemErr: "err3", DurationMs: 12,
		Result: types.Result{Status: types.StatusError, Message: "msg3", Type: "type3", Desc: "desc3"}}
	tdb := &TimeScaleDb{Conn: db, Log: log, EvalTable: table}
	err = tdb.Write(ctx, account, org, project, pipeline, build, stage, step, report, repo, sha, test1, test2, test3)
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
		SELECT duration_ms, result, name FROM %s WHERE account_id = $1
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
	tdb := &TimeScaleDb{Conn: db, Log: log, EvalTable: table}
	got, err := tdb.Summary(ctx, account, org, project, pipeline, build, report)
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
		SELECT name, suite_name, class_name, duration_ms, result, message,
		description, type, stdout, stderr, COUNT(*) OVER() AS full_count
		FROM %s
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND report = $6 AND suite_name = $7 AND result IN (%s)
		ORDER BY %s %s, %s %s
		LIMIT $8 OFFSET $9;`, table, "'failed', 'error'", "duration_ms", "DESC", "name", "ASC")
	query = regexp.QuoteMeta(query)
	mock.ExpectQuery(query).
		WithArgs(account, org, project, pipeline, build, report, suite, defaultLimit, defaultOffset).WillReturnRows(rows)
	tdb := &TimeScaleDb{Conn: db, Log: log, EvalTable: table}
	got, err := tdb.GetTestCases(ctx, account, org, project, pipeline, build, report, suite, "duration_ms", "failed", desc, "", "")
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
		SUM(CASE WHEN result = 'skipped' THEN 1 ELSE 0 END) AS skipped_tests,
		SUM(CASE WHEN result = 'passed' THEN 1 ELSE 0 END) AS passed_tests,
		SUM(CASE WHEN result = 'failed' OR result = 'error' THEN 1 ELSE 0 END) AS failed_tests,
		SUM(CASE WHEN result = 'failed' OR result = 'error' THEN 1 ELSE 0 END) * 100 / COUNT(*) AS fail_pct,
		COUNT(*) OVER() AS full_count
		FROM %s
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND report = $6 AND result IN (%s)
		GROUP BY suite_name
		ORDER BY %s %s, %s %s
		LIMIT $7 OFFSET $8;`, table, "'failed', 'error', 'passed', 'skipped'", "fail_pct", desc, "suite_name", asc)
	query = regexp.QuoteMeta(query)
	mock.ExpectQuery(query).
		WithArgs(account, org, project, pipeline, build, report, defaultLimit, defaultOffset).WillReturnRows(rows)
	tdb := &TimeScaleDb{Conn: db, Log: log, EvalTable: table}
	got, err := tdb.GetTestSuites(ctx, account, org, project, pipeline, build, report, "", "", "", "", "")
	li, _ := strconv.Atoi(defaultLimit)
	assert.Nil(t, err, nil)
	assert.Equal(t, got.Metadata.TotalPages, 1)
	assert.Equal(t, got.Metadata.TotalItems, 2)
	assert.Equal(t, got.Metadata.PageItemCount, 2)
	assert.Equal(t, got.Metadata.PageSize, li)
	assert.ElementsMatch(t, got.Suites, suites)
}

func Test_WriteSelectedTests(t *testing.T) {
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

	total := 20
	selected := 10
	new := 5
	updated := 3
	src := 2

	arg := types.SelectTestsResp{
		TotalTests:    total,
		SelectedTests: selected,
		NewTests:      new,
		UpdatedTests:  updated,
		SrcCodeTests:  src,
		SelectAll:     false,
		Tests:         nil,
	}

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}
	valueStrings := constructPsqlInsertStmt(1, 12)
	stmt := fmt.Sprintf(
		`
				INSERT INTO %s
				(account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id,
				test_count, test_selected, source_code_test, new_test, updated_test)
				VALUES %s`, table, valueStrings)
	stmt = regexp.QuoteMeta(stmt)
	mock.ExpectExec(stmt).
		WithArgs(account, org, project, pipeline, build, stage, step,
			total, selected, src, new, updated).WillReturnResult(sqlmock.NewResult(0, 1))
	tdb := &TimeScaleDb{Conn: db, Log: log, SelectionTable: table}
	err = tdb.WriteSelectedTests(ctx, account, org, project, pipeline, build, stage, step, arg, false)
	assert.Nil(t, err, nil)
}

func Test_WriteSelectedTests_WithUpsert(t *testing.T) {
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

	total := 20
	selected := 10
	new := 5
	updated := 3
	src := 2

	arg := types.SelectTestsResp{
		TotalTests:    total,
		SelectedTests: selected,
		NewTests:      new,
		UpdatedTests:  updated,
		SrcCodeTests:  src,
		SelectAll:     false,
		Tests:         nil,
	}

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}

	stmt := fmt.Sprintf(
		`
				UPDATE %s
				SET test_count = test_count + $1, test_selected = test_selected + $2,
				source_code_test = source_code_test + $3, new_test = new_test + $4, updated_test = updated_test + $5
				WHERE account_id = $6 AND org_id = $7 AND project_id = $8 AND pipeline_id = $9 AND build_id = $10
				`, table)
	stmt = regexp.QuoteMeta(stmt)
	mock.ExpectExec(stmt).
		WithArgs(total, selected, src, new, updated,
			account, org, project, pipeline, build).WillReturnResult(sqlmock.NewResult(0, 1))
	tdb := &TimeScaleDb{Conn: db, Log: log, SelectionTable: table}
	err = tdb.WriteSelectedTests(ctx, account, org, project, pipeline, build, stage, step, arg, true)
	assert.Nil(t, err, nil)
}
