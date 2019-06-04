PROJECT = mpv_remote_app
PYTHON  = python3

all: build

build: setup.py $(PROJECT)
	$(PYTHON) setup.py sdist bdist_wheel

install: build
	sudo $(PYTHON) setup.py install --record .install-files
uninstall: setup.py .install-files
	cat .install-files | xargs sudo rm -rf
	rm -f .install-files

push: dist
	twine upload dist/*
push-test: dist
	twine upload --repository-url https://test.pypi.org/legacy/ dist/*

clean:
	rm -rf build dist *.egg-info $(PROJECT)/*.pyc

.phony: build push clean
