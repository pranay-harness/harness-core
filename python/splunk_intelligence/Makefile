SHELL := /bin/bash

version := $(shell cat 'VERSION')
dist_file = splunk_pyml-$(version).tar

clean:
	if [ -d ".pyenv" ]; then rm -rf .pyenv; fi

init-orig:
	if [ ! -d ".pyenv" ]; then echo "Setting up virtualenv"; virtualenv --python=/usr/bin/python2.7 .pyenv; fi
	source .pyenv/bin/activate ; pip install --upgrade pip; pip install -r requirements.txt.orig;

init:
	if [ ! -d ".pyenv" ]; then echo "Setting up virtualenv"; virtualenv --python=/usr/bin/python2.7 .pyenv; fi
	source .pyenv/bin/activate ; pip install --upgrade pip; pip install -r requirements.txt;

init-onprem-orig:
	if [ ! -d ".pyenv" ]; then echo "Setting up virtualenv"; virtualenv --python=/usr/bin/python2.7 .pyenv; fi
	source .pyenv/bin/activate ; pip install --upgrade pip; pip install -r requirements-onprem.txt.orig;

init-onprem:
	if [ ! -d ".pyenv" ]; then echo "Setting up virtualenv"; virtualenv --python=/usr/bin/python2.7 .pyenv; fi
	source .pyenv/bin/activate ; pip install --upgrade pip; pip install -r requirements-onprem.txt;

init-docker:
	pip install --upgrade pip; pip install -r requirements.txt;

init-docker-onprem:
	pip install --upgrade pip; pip install -r requirements-onprem.txt;

test:
	source .pyenv/bin/activate ; cd tests; nosetests -vv --nocapture .

jupyter:
	source .pyenv/bin/activate ; jupyter nbextension enable --py --sys-prefix widgetsnbextension; jupyter notebook --NotebookApp.iopub_data_rate_limit=100000000

compile_clean:
	find . -name "*.pyc" -type f -delete

compile:
	source .pyenv/bin/activate; python -m compileall -q src

dist: compile
	pwd
	echo $(version)
	rm -rf dist; mkdir -p dist/splunk_pyml; cp -L service_version.properties dist/splunk_pyml; cp Dockerfile dist/splunk_pyml; cp Dockerfile-onprem dist/splunk_pyml; cp Makefile dist/splunk_pyml; cp *.ini dist/splunk_pyml; cp *.sh dist/splunk_pyml; cp requirements.txt dist/splunk_pyml; cp requirements-onprem.txt dist/splunk_pyml
	ln -s ../../.pyenv dist/splunk_pyml/.pyenv
	cd src; find . -name '*.pyc' | cpio -updm ../dist/splunk_pyml; cd ../dist; pwd; tar --exclude='splunk_pyml/.pyenv' -cvf $(dist_file) *
