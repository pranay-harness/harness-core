package handler

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/wings-software/portal/product/ci/ti-service/config"
	"github.com/wings-software/portal/product/ci/ti-service/tidb"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.uber.org/zap"
)

// HandleSelect returns an http.HandlerFunc that figures out which tests to run
// based on the files provided.
func HandleSelect(tidb tidb.TiDB, config config.Config, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()

		// TODO: Use this information while retrieving from TIDB
		err := validate(r, accountIDParam, repoParam)
		if err != nil {
			WriteInternalError(w, err)
			return
		}
		accountId := r.FormValue(accountIDParam)
		repo := r.FormValue(repoParam)

		var files []string
		if err := json.NewDecoder(r.Body).Decode(&files); err != nil {
			WriteBadRequest(w, err)
			log.Errorw("api: could not unmarshal input for test selection",
				"account_id", accountId, "repo", repo, zap.Error(err))
			return
		}

		var tests []types.Test
		log.Infow(fmt.Sprintf("tests received are: %s", tests))
		if tests, err = tidb.GetTestsToRun(ctx, files); err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: could not select tests", "account_id", accountId,
				"repo", repo, zap.Error(err))
			return
		}

		WriteJSON(w, tests, 200)
		log.Infow("completed test selection", "account_id", accountId,
			"repo_name", repo, "num_tests", len(tests), "time_taken", time.Since(st))

	}
}
