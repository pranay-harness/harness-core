package handler

import (
	"errors"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"net/http"
	"strings"
	"time"

	"github.com/wings-software/portal/product/ci/ti-service/config"
	"github.com/wings-software/portal/product/ci/ti-service/logger"
)

// HandleDownloadLink returns an http.HandlerFunc that returns a list of
// downloadable link for the agent artifacts
func HandleDownloadLink(config config.Config) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()
		log := logger.FromContext(ctx)
		// Args: language, os, arch, framework
		err := validate(r, languageParam)
		if err != nil {
			WriteInternalError(w, err)
			return
		}

		language := r.FormValue(languageParam)
		os := r.FormValue(osParam)
		accountId := r.FormValue(accountIDParam)
		baseURL := config.Agent.BaseUrl
		if !strings.HasSuffix(baseURL, "/") {
			baseURL = baseURL + "/"
		}

		var links []types.DownloadLink
		switch language {
		case "java":
			links = append(links, types.DownloadLink{URL: baseURL + "java/java-agent.jar", RelPath: "java/java-agent.jar"})
		case "csharp":
			// TODO: (Vistaar) Add support for different OS/framework/architectures here once we have it
			links = append(links, types.DownloadLink{URL: baseURL + "csharp/dotnet-agent.zip", RelPath: "csharp/dotnet-agent.zip"})
		default:
			err = errors.New("language not supported")
		}
		if err != nil {
			WriteInternalError(w, err)
			return
		}
		WriteJSON(w, links, 200)
		log.Infow("got download link", "account_id", accountId, "language", language, "os", os, "time_taken", time.Since(st))
	}
}
