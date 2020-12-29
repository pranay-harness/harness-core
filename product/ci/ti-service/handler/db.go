package handler

import (
	"encoding/json"
	"fmt"
	"go.uber.org/zap"
	"net/http"
	"strconv"
	"time"

	"github.com/wings-software/portal/product/ci/ti-service/config"
	"github.com/wings-software/portal/product/ci/ti-service/db"
	"github.com/wings-software/portal/product/ci/ti-service/types"
)

// HandleWrite returns an http.HandlerFunc that writes test information to the DB
func HandleWrite(db db.Db, config config.Config, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, buildIdParam, stageIdParam, stepIdParam, reportParam)
		if err != nil {
			WriteInternalError(w, err)
			return
		}

		accountId := r.FormValue(accountIDParam)
		orgId := r.FormValue(orgIdParam)
		projectId := r.FormValue(projectIdParam)
		buildId := r.FormValue(buildIdParam)
		stageId := r.FormValue(stageIdParam)
		stepId := r.FormValue(stepIdParam)
		report := r.FormValue(reportParam)

		var in []*types.TestCase
		if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
			WriteBadRequest(w, err)
			log.Errorw("api: cannot unmarshal input", "account_id", accountId, "org_id", orgId,
				"project_id", projectId, "build_id", buildId, "stage_id", stageId, zap.Error(err))
			return
		}

		if err := db.Write(ctx, config.TimeScaleDb.HyperTableName, accountId, orgId, projectId, buildId, stageId, stepId, report, in...); err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: cannot write to db", "account_id", accountId, "org_id", orgId,
				"project_id", projectId, "build_id", buildId, zap.Error(err))
			return
		}

		w.WriteHeader(http.StatusNoContent)
		log.Infow("wrote test cases", "account_id", accountId, "org_id", orgId,
			"project_id", projectId, "build_id", buildId, "stage_id", stageId, "step_id", stepId,
			"num_cases", len(in), "time_taken", time.Since(st))
	}
}

// HandleSummary returns an http.HandlerFunc that summarises test reports.
func HandleSummary(adb db.Db, config config.Config, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()
		st := time.Now()
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, buildIdParam, reportParam)
		if err != nil {
			WriteInternalError(w, err)
			return
		}

		accountId := r.FormValue(accountIDParam)
		orgId := r.FormValue(orgIdParam)
		projectId := r.FormValue(projectIdParam)
		buildId := r.FormValue(buildIdParam)
		report := r.FormValue(reportParam)

		var resp types.SummaryResponse

		resp, err = adb.Summary(ctx, config.TimeScaleDb.HyperTableName, accountId, orgId, projectId, buildId, report)
		if err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: cannot get summary from DB", "account_id", accountId, "org_id", orgId,
				"project_id", projectId, "build_id", buildId, zap.Error(err))
			return
		}

		WriteJSON(w, resp, 200)
		log.Infow("retrieved summary", "account_id", accountId, "org_id", orgId,
			"project_id", projectId, "build_id", buildId, "time_taken", time.Since(st))
	}
}

// HandleTestCases returns an http.HandlerFunc that returns test case information.
func HandleTestCases(adb db.Db, config config.Config, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()
		st := time.Now()
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, buildIdParam, suiteNameParam, reportParam)
		if err != nil {
			WriteInternalError(w, err)
			return
		}

		accountId := r.FormValue(accountIDParam)
		orgId := r.FormValue(orgIdParam)
		projectId := r.FormValue(projectIdParam)
		buildId := r.FormValue(buildIdParam)
		suite := r.FormValue(suiteNameParam)
		sort := r.FormValue(sortParam)
		status := r.FormValue(statusParam)
		pageSize := r.FormValue(pageSizeParam)
		pageIndex := r.FormValue(pageIndexParam)
		order := r.FormValue(orderParam)
		report := r.FormValue(reportParam)

		if pageSize == "" {
			pageSize = "10"
		}

		if pageIndex == "" {
			pageIndex = "0"
		}

		ps, err := strconv.Atoi(pageSize)
		if err != nil {
			WriteInternalError(w, err)
			return
		}

		pi, err := strconv.Atoi(pageIndex)
		if err != nil {
			WriteInternalError(w, err)
			return
		}

		resp, err := adb.GetTestCases(ctx, config.TimeScaleDb.HyperTableName, accountId, orgId, projectId, buildId, report, suite, sort, status, order, pageSize, strconv.Itoa(pi*ps))
		if err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: cannot get test cases from DB", "account_id", accountId, "org_id", orgId,
				"project_id", projectId, "build_id", buildId, zap.Error(err))
			return
		}

		WriteJSON(w, resp, 200)
		log.Infow("retrieved test cases", "account_id", accountId, "org_id", orgId,
			"project_id", projectId, "build_id", buildId, "time_taken", time.Since(st))
	}
}

// HandleTestCases returns an http.HandlerFunc that return test suite information.
func HandleTestSuites(adb db.Db, config config.Config, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()
		st := time.Now()
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, buildIdParam, reportParam)
		if err != nil {
			WriteInternalError(w, err)
			return
		}

		accountId := r.FormValue(accountIDParam)
		orgId := r.FormValue(orgIdParam)
		projectId := r.FormValue(projectIdParam)
		buildId := r.FormValue(buildIdParam)
		sort := r.FormValue(sortParam)
		status := r.FormValue(statusParam)
		pageSize := r.FormValue(pageSizeParam)
		pageIndex := r.FormValue(pageIndexParam)
		order := r.FormValue(orderParam)
		report := r.FormValue(reportParam)

		if pageSize == "" {
			pageSize = defaultPageSize
		}

		if pageIndex == "" {
			pageIndex = defaultPageIndex
		}

		ps, err := strconv.Atoi(pageSize)
		if err != nil {
			WriteInternalError(w, err)
			return
		}

		pi, err := strconv.Atoi(pageIndex)
		if err != nil {
			WriteInternalError(w, err)
			return
		}

		resp, err := adb.GetTestSuites(ctx, config.TimeScaleDb.HyperTableName, accountId, orgId, projectId, buildId, report, sort, status, order, pageSize, strconv.Itoa(pi*ps))
		if err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: cannot get test suite information from DB", "account_id", accountId, "org_id", orgId,
				"project_id", projectId, "build_id", buildId, zap.Error(err))
			return
		}

		WriteJSON(w, resp, 200)
		log.Infow("retrieved test suites", "account_id", accountId, "org_id", orgId,
			"project_id", projectId, "build_id", buildId, "time_taken", time.Since(st))
	}
}

// validate checks required form values and writes errors if they are not set
func validate(r *http.Request, ls ...string) error {
	for _, s := range ls {
		fv := r.FormValue(s)
		if fv == "" {
			return fmt.Errorf("required field %s not present in query param", s)
		}
	}
	return nil
}
