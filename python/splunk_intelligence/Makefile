SHELL := /bin/bash

clean:
	if [ -d ".pyenv" ]; then rm -rf .pyenv; fi
init:
	if [ ! -d ".pyenv" ]; then echo "Setting up virtualenv"; virtualenv --python=/usr/bin/python2.7 .pyenv; fi
	source .pyenv/bin/activate ; pip install -r requirements.txt;
test:
	source .pyenv/bin/activate ; nosetests -vv tests/; deactivate

jupyter:
	source .pyenv/bin/activate ; jupyter nbextension enable --py --sys-prefix widgetsnbextension; jupyter notebook --NotebookApp.iopub_data_rate_limit=100000000
