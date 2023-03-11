SRC      = mpv_remote_app
PY_FILES = $(shell find $(SRC) -type f -name '*.py')
PYTHON   = python3
PKG      = mpv-remote-app

.DEFAULT_GOAL := build

.PHONY: build
build: dist
	$(PYTHON) setup.py sdist bdist_wheel

dist: setup.py $(PY_FILES)
	$(PYTHON) -m build

.PHONY: test
test: $(PY_FILES)
	$(PYTHON) -m unittest discover tests

.PHONY: install
install: build
	$(PYTHON) -m pip install .

.PHONY: uninstall
uninstall: setup.py
	$(PYTHON) -m pip uninstall $(PKG)

.PHONY: push
push: dist
	$(PYTHON) -m twine upload dist/*

.PHONY: push-test
push-test: dist
	$(PYTHON) -m twine upload --repository testpypi dist/*

.PHONY: clean
clean:
	rm -rf build dist $(SRC)/*.egg-info $(SRC)/*.pyc
