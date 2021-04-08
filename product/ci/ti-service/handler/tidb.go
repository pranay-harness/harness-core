package handler

import (
	"encoding/json"
	"fmt"
	"github.com/wings-software/portal/product/ci/addon/ti"
	"github.com/wings-software/portal/product/ci/common/avro"
	"github.com/wings-software/portal/product/ci/ti-service/config"
	"github.com/wings-software/portal/product/ci/ti-service/db"
	"github.com/wings-software/portal/product/ci/ti-service/tidb"
	"github.com/wings-software/portal/product/ci/ti-service/tidb/mongodb"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.uber.org/zap"
	"net/http"
	"time"
)

const (
	cgSchemaPath = "callgraph.avsc"
)

// HandleSelect returns an http.HandlerFunc that figures out which tests to run
// based on the files provided.
func HandleSelect(tidb tidb.TiDB, db db.Db, config config.Config, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()

		// TODO: Use this information while retrieving from TIDB
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, pipelineIdParam, buildIdParam,
			stageIdParam, stepIdParam, repoParam, shaParam, branchParam)
		if err != nil {
			WriteInternalError(w, err)
			return
		}
		accountId := r.FormValue(accountIDParam)
		orgId := r.FormValue(orgIdParam)
		projectId := r.FormValue(projectIdParam)
		pipelineId := r.FormValue(pipelineIdParam)
		buildId := r.FormValue(buildIdParam)
		stageId := r.FormValue(stageIdParam)
		stepId := r.FormValue(stepIdParam)
		repo := r.FormValue(repoParam)
		branch := r.FormValue(branchParam)
		sha := r.FormValue(shaParam)

		var req types.SelectTestsReq
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			WriteBadRequest(w, err)
			log.Errorw("api: could not unmarshal input for test selection",
				"account_id", accountId, "repo", repo, "branch", branch, "sha", sha, zap.Error(err))
			return
		}
		log.Infow("got a files list", "account_id", accountId, "files", req.Files, "repo", repo, "branch", branch, "sha", sha)

		// Make call to Mongo DB to get the tests to run
		selected, err := tidb.GetTestsToRun(ctx, req)
		if err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: could not select tests", "account_id", accountId,
				"repo", repo, "branch", branch, "sha", sha, zap.Error(err))
			return
		}

		// Write changed file information to timescaleDB
		err = db.WriteDiffFiles(ctx, config.TimeScaleDb.CoverageTable, accountId, orgId,
			projectId, pipelineId, buildId, stageId, stepId, types.DiffInfo{Sha: sha, Files: req.Files})
		if err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: could not write changed file information", "account_id", accountId,
				"repo", repo, "branch", branch, "sha", sha, zap.Error(err))
		}

		// Classify and write the test selection stats to timescaleDB
		err = db.WriteSelectedTests(ctx, config.TimeScaleDb.SelectionTable, accountId, orgId,
			projectId, pipelineId, buildId, stageId, stepId, selected)
		if err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: could not write selected tests", "account_id", accountId,
				"repo", repo, "branch", branch, "sha", sha, zap.Error(err))
			return
		}

		// Write the selected tests back
		WriteJSON(w, selected, 200)
		log.Infow("completed test selection", "account_id", accountId,
			"repo", repo, "branch", branch, "sha", sha, "tests", selected.Tests,
			"num_tests", len(selected.Tests), "time_taken", time.Since(st))

	}
}

func HandleOverview(db db.Db, config config.Config, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()

		// TODO: Use this information while retrieving from TIDB
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, pipelineIdParam, buildIdParam)
		if err != nil {
			WriteInternalError(w, err)
			return
		}
		accountId := r.FormValue(accountIDParam)
		orgId := r.FormValue(orgIdParam)
		projectId := r.FormValue(projectIdParam)
		pipelineId := r.FormValue(pipelineIdParam)
		buildId := r.FormValue(buildIdParam)

		overview, err := db.GetSelectionOverview(ctx, config.TimeScaleDb.SelectionTable, accountId,
			orgId, projectId, pipelineId, buildId)
		if err != nil {
			log.Errorw("could not get TI overview from DB", zap.Error(err))
			WriteInternalError(w, err)
			return
		}

		// Write the selected tests back
		WriteJSON(w, overview, 200)
		log.Infow("retrieved test overview", "account_id", accountId, "time_taken", time.Since(st))

	}
}

func HandleUploadCg(tidb tidb.TiDB, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, repoParam, branchParam, shaParam)
		if err != nil {
			WriteBadRequest(w, err)
			return
		}
		acc := r.FormValue(accountIDParam)
		org := r.FormValue(orgIdParam)
		proj := r.FormValue(projectIdParam)
		info := mongodb.VCSInfo{
			Repo:     r.FormValue(repoParam),
			Branch:   r.FormValue(branchParam),
			CommitId: r.FormValue(shaParam),
		}
		var data []byte
		if err := json.NewDecoder(r.Body).Decode(&data); err != nil {
			log.Errorw("could not unmarshal request body")
			WriteBadRequest(w, err)
		}
		cgSer, err := avro.NewCgphSerialzer(cgSchemaPath)
		if err != nil {
			log.Errorw("failed to create callgraph serializer instance", accountIDParam, acc, repoParam, info.Repo, branchParam, info.Branch, zap.Error(err))
			WriteBadRequest(w, err)
			return
		}
		cgString, err := cgSer.Deserialize(data)
		if err != nil {
			log.Errorw("failed to deserialize callgraph", accountIDParam, acc, repoParam, info.Repo, branchParam, info.Branch, zap.Error(err))
			WriteBadRequest(w, err)
			return
		}
		cg, err := ti.FromStringMap(cgString.(map[string]interface{}))
		if err != nil {
			log.Errorw("failed to construct callgraph object from interface object", accountIDParam, acc, repoParam, info.Repo, branchParam, info.Branch, zap.Error(err))
			WriteBadRequest(w, err)
			return
		}
		log.Infow(fmt.Sprintf("received %d nodes and %d relations", len(cg.Nodes), len(cg.Relations)), accountIDParam, acc, repoParam, info.Repo, branchParam, info.Branch)
		err = tidb.UploadPartialCg(r.Context(), cg, info, acc, org, proj)
		if err != nil {
			log.Errorw("failed to write callgraph to db", accountIDParam, acc, repoParam, info.Repo, branchParam, info.Branch, zap.Error(err))
			WriteBadRequest(w, err)
			return
		}
		w.WriteHeader(http.StatusNoContent)
	}
}
