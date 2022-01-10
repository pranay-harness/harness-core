// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package logs

import (
	"github.com/blendle/zapdriver"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"strings"
)

// A Builder is used to build a logger.
// Access to the underlying zap.Config is granted if a helper does not exist for the required functionality.
type Builder struct {
	Config zap.Config
}

// NewBuilder returns a new Builder with the minimum default configuration, using a reasonable production configuration.
func NewBuilder() *Builder {
	var b Builder
	b.Config = zapdriver.NewProductionConfig()
	b.applyDefaults()
	return &b
}

// NewDevelopmentBuilder returns a new Builder with the minimum default configuration, using a reasonable development configuration.
func NewDevelopmentBuilder() *Builder {
	var b Builder
	b.Config = zap.NewDevelopmentConfig()
	b.applyDefaults()
	return &b
}

// applyDefaults applies the Harness standard logging defaults:
// * ts must have the timestamp in RFC3339 format
// * application_id must be the application logging the event
// * session_id must be the current session
// * customer_id must be the customer
// * level must be one of { debug, info, warn, error, fatal }
// * deployable must be the deployable logging the event
// * deployment must be the deployment label
// * environment should be one of { prod, stage, qa, dev }
func (b *Builder) applyDefaults() {
	// Example for zap's ISO8601: "2020-05-07T15:09:05.000Z0900"
	b.Config.EncoderConfig.EncodeTime = zapcore.ISO8601TimeEncoder
	b.Config.EncoderConfig.TimeKey = "ts"
	b.Config.InitialFields = map[string]interface{}{}
}

// Build builds the logger out of the underlying config
func (b *Builder) Build() (*zap.Logger, error) {
	log, err := b.Config.Build()
	if err != nil {
		return nil, err
	}
	return log, nil
}

// MustBuild builds the logger out of the underlying config, panics if it fails
func (b *Builder) MustBuild() *zap.Logger {
	log, err := b.Build()
	if err != nil {
		panic(err)
	}
	return log
}

// Verbose sets the log level to zap.DebugLevel if verbose == true, or performs a noop if verbose == false.
func (b *Builder) Verbose(verbose bool) *Builder {
	if verbose {
		return b.WithLevel(zap.DebugLevel)
	}
	return b
}

// WithLevel sets the level of the logger to the given zapcore.Level
func (b *Builder) WithLevel(level zapcore.Level) *Builder {
	b.Config.Level.SetLevel(level)
	return b
}

// WithFields adds fields to the initial fields of the logger.
// Calling this with an odd number of arguments is undefined and will panic.
func (b *Builder) WithFields(kvps ...interface{}) *Builder {
	if len(kvps)%2 != 0 {
		panic("odd number of key values provided to WithFields when building logger")
	}

	// store odd cdIndex content as key, even cdIndex content as value
	for i := 0; i < len(kvps)-1; i += 2 {
		key, ok := kvps[i].(string)
		if !ok {
			panic("non string value for key provided to WithFields when building logger")
		}

		b.Config.InitialFields[key] = kvps[i+1]
	}
	return b
}

// WithDeployment adds a field "deployment" set to the passed string to the initial fields of the logger.
// Also adds the "environment" field based off of the given deployment.
func (b *Builder) WithDeployment(deployment string) *Builder {
	b.Config.InitialFields["deployment"] = deployment
	b.Config.InitialFields["environment"] = ExtractEnvironment(deployment)
	return b
}

// ExtractEnvironment extracts environment from deployment name
func ExtractEnvironment(deployment string) string {
	env := "dev"
	if strings.HasPrefix(deployment, "prod") {
		env = "prod"
	} else if strings.HasPrefix(deployment, "stage") {
		env = "stage"
	} else if strings.HasPrefix(deployment, "qa") {
		env = "qa"
	}
	return env
}
