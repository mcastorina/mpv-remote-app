# Makefile for Android projects

GRADLE_BIN := ./gradlew
APK_DIR := app/build/outputs/apk
SRC_DIR := app/src/main
TARGET := $(APK_DIR)/app-debug.apk

SOURCES := $(shell find $(SRC_DIR) -type f)
PACKAGE := $(shell cat $(SRC_DIR)/AndroidManifest.xml | grep 'package=' | awk -F '"' '{print $$2}')

TOOLS_DIR := ~/tmp/android/tools
EMULATOR_BIN := $(TOOLS_DIR)/emulator
EMULATOR_DEV := Nexus_5_API_25

all: $(TARGET)

$(TARGET): $(SOURCES)
	@$(GRADLE_BIN) assembleDebug
	@touch $(TARGET)

install: $(TARGET)
	@adb install $(TARGET)

uninstall:
	@adb uninstall $(PACKAGE)

reinstall: $(TARGET)
	-@adb uninstall $(PACKAGE)
	@adb install $(TARGET)

emulator:
	@$(EMULATOR_BIN) -avd $(EMULATOR_DEV) &> /dev/null &

clean:
	rm -f $(TARGET)

super-clean:
	rm -rf app/build

.phony: install uninstall reinstall emulator clean super-clean
