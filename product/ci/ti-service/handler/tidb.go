package handler

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/wings-software/portal/product/ci/addon/ti"
	"github.com/wings-software/portal/product/ci/common/avro"
	"github.com/wings-software/portal/product/ci/ti-service/db"
	"github.com/wings-software/portal/product/ci/ti-service/tidb"
	"github.com/wings-software/portal/product/ci/ti-service/tidb/mongodb"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.uber.org/zap"
)

const (
	cgSchemaPath = "callgraph.avsc"
)

// HandleSelect returns an http.HandlerFunc that figures out which tests to run
// based on the files provided.
func HandleSelect(tidb tidb.TiDB, db db.Db, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()

		// TODO: Use this information while retrieving from TIDB
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, pipelineIdParam, buildIdParam,
			stageIdParam, stepIdParam, repoParam, targetBranchParam)
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
		source := r.FormValue(sourceBranchParam)
		target := r.FormValue(targetBranchParam)
		sha := r.FormValue(shaParam)

		var req types.SelectTestsReq
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			WriteBadRequest(w, err)
			log.Errorw("api: could not unmarshal input for test selection",
				"account_id", accountId, "repo", repo, "source", source, "target", target,
				"sha", sha, zap.Error(err))
			return
		}
		req.TargetBranch = target
		req.Repo = repo
		log.Infow("got a files list", "account_id", accountId, "files", req.Files,
			"repo", repo, "source", source, "target", target, "sha", sha)

		// Make call to Mongo DB to get the tests to run
		selected, err := tidb.GetTestsToRun(ctx, req, accountId)
		if err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: could not select tests", "account_id", accountId,
				"repo", repo, "source", source, "target", target, "sha", sha, zap.Error(err))
			return
		}

		// Classify and write the test selection stats to timescaleDB
		err = db.WriteSelectedTests(ctx, accountId, orgId, projectId, pipelineId, buildId, stageId, stepId, selected, false)
		if err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: could not write selected tests", "account_id", accountId,
				"repo", repo, "source", source, "target", target, "sha", sha, zap.Error(err))
			return
		}

		// Write the selected tests back
		WriteJSON(w, selected, 200)
		log.Infow("completed test selection", "account_id", accountId,
			"repo", repo, "source", source, "target", target, "sha", sha, "tests",
			selected.Tests, "num_tests", len(selected.Tests), "time_taken", time.Since(st))

	}
}

func HandleReportsInfo(db db.Db, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()

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

		resp, err := db.GetReportsInfo(ctx, accountId, orgId, projectId, pipelineId, buildId)
		if err != nil {
			log.Errorw("could not get reports info from DB", zap.Error(err))
			WriteInternalError(w, err)
			return
		}

		// Write the selected tests back
		WriteJSON(w, resp, 200)
		log.Infow("retrieved test report info", "account_id", accountId, "time_taken", time.Since(st))
	}
}

func HandleIntelligenceInfo(db db.Db, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()

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

		resp, err := db.GetIntelligenceInfo(ctx, accountId, orgId, projectId, pipelineId, buildId)
		if err != nil {
			log.Errorw("could not get test intelligence info from DB", zap.Error(err))
			WriteInternalError(w, err)
			return
		}

		// Write the selected tests back
		WriteJSON(w, resp, 200)
		log.Infow("retrieved test intelligence info", "account_id", accountId, "time_taken", time.Since(st))
	}
}

func HandleOverview(db db.Db, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()

		// TODO: Use this information while retrieving from TIDB
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, pipelineIdParam, buildIdParam, stepIdParam, stageIdParam)
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

		overview, err := db.GetSelectionOverview(ctx, accountId, orgId, projectId, pipelineId, buildId, stepId, stageId)
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

func HandleUploadCg(tidb tidb.TiDB, db db.Db, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, repoParam, sourceBranchParam, targetBranchParam)
		if err != nil {
			WriteBadRequest(w, err)
			return
		}
		acc := r.FormValue(accountIDParam)
		org := r.FormValue(orgIdParam)
		proj := r.FormValue(projectIdParam)
		target := r.FormValue(targetBranchParam)
		pipelineId := r.FormValue(pipelineIdParam)
		buildId := r.FormValue(buildIdParam)
		stageId := r.FormValue(stageIdParam)
		stepId := r.FormValue(stepIdParam)
		info := mongodb.VCSInfo{
			Repo:     r.FormValue(repoParam),
			Branch:   r.FormValue(sourceBranchParam),
			CommitId: r.FormValue(shaParam),
		}
		var data []byte
		if err := json.NewDecoder(r.Body).Decode(&data); err != nil {
			log.Errorw("could not unmarshal request body")
			WriteBadRequest(w, err)
		}
		cgSer, err := avro.NewCgphSerialzer(cgSchemaPath)
		if err != nil {
			log.Errorw("failed to create callgraph serializer instance", accountIDParam, acc,
				repoParam, info.Repo, sourceBranchParam, info.Branch, zap.Error(err))
			WriteBadRequest(w, err)
			return
		}
		cgString, err := cgSer.Deserialize(data)
		if err != nil {
			log.Errorw("failed to deserialize callgraph", accountIDParam, acc, repoParam, info.Repo,
				sourceBranchParam, info.Branch, zap.Error(err))
			WriteBadRequest(w, err)
			return
		}
		cg, err := ti.FromStringMap(cgString.(map[string]interface{}))
		if err != nil {
			log.Errorw("failed to construct callgraph object from interface object",
				accountIDParam, acc, repoParam, info.Repo, sourceBranchParam, info.Branch, zap.Error(err))
			WriteBadRequest(w, err)
			return
		}
		log.Infow(fmt.Sprintf("received %d nodes and %d relations", len(cg.Nodes), len(cg.Relations)),
			accountIDParam, acc, repoParam, info.Repo, sourceBranchParam, info.Branch, targetBranchParam, target)
		resp, err := tidb.UploadPartialCg(r.Context(), cg, info, acc, org, proj, target)
		// Try to update counts even if uploading partial CG failed
		werr := db.WriteSelectedTests(r.Context(), acc, org, proj, pipelineId, buildId, stageId, stepId, resp, true)
		if err != nil {
			log.Errorw("failed to write callgraph to db", accountIDParam, acc, repoParam, info.Repo,
				sourceBranchParam, info.Branch, zap.Error(err))
			WriteBadRequest(w, err)
			return
		} else if werr != nil {
			log.Errorw("failed to update test counts in stats DB", accountIDParam, acc, orgIdParam, org, repoParam, info.Repo,
				buildIdParam, buildId, zap.Error(werr))
			WriteBadRequest(w, werr)
			return
		}
		w.WriteHeader(http.StatusNoContent)
	}
}
