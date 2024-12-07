setup:
	gradle wrapper --gradle-version 8.8

clean:
	./gradlew clean


build:
	./gradlew clean build

run:
	./gradlew run --args="-h"

lint:
	./gradlew checkstyleMain checkstyleTest

test:
	./gradlew test

report:
	./gradlew jacocoTestReport

install:
	./gradlew clean install

run-dist:
	./build/install/app/bin/app -h

check-updates:
	./gradlew dependencyUpdates

.PHONY: build