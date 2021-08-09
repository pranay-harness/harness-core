package handler

import (
	"io"
	"net/http"
	"time"

	"github.com/dchest/authcookie"
	"github.com/wings-software/portal/product/log-service/config"
)

const defaultTokenExpiryTime = 48 * time.Hour

// HandleToken returns back a token using the inbuilt account ID
func HandleToken(config config.Config) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		h := w.Header()
		h.Set("Access-Control-Allow-Origin", "*")
		secret := []byte(config.Secrets.LogSecret)
		accountID := r.FormValue(accountIDParam)
		cookie := authcookie.NewSinceNow(accountID, defaultTokenExpiryTime, secret)
		io.WriteString(w, cookie)
	}
}
