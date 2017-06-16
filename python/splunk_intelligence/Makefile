SHELL := /bin/bash

version := $(shell cat 'VERSION')
dist_file = splunk_pyml-$(version).tar

clean:
	if [ -d ".pyenv" ]; then rm -rf .pyenv; fi
init:
	if [ ! -d ".pyenv" ]; then echo "Setting up virtualenv"; virtualenv --python=/usr/bin/python2.7 .pyenv; fi
	source .pyenv/bin/activate ; pip install --upgrade pip; pip install -r requirements.txt;
test:
	source .pyenv/bin/activate ; nosetests -vv tests/; deactivate

jupyter:
	source .pyenv/bin/activate ; jupyter nbextension enable --py --sys-prefix widgetsnbextension; jupyter notebook --NotebookApp.iopub_data_rate_limit=100000000

compile_clean:
	find . -name "*.pyc" -type f -delete

compile:
	source .pyenv/bin/activate; python -m compileall src

dist: compile
	pwd
	echo $(version)
	rm -rf dist; mkdir -p dist/splunk_pyml; cd src; find . -name '*.pyc' | cpio -updm ../dist/splunk_pyml; cd ../dist; pwd; tar -cvf $(dist_file) *
