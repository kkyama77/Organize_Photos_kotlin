#!/bin/bash
# Version bump script for CI/CD
# Usage: ./bump-version.sh patch|minor|major

set -e

GRADLE_PROPS="gradle.properties"

# Read current version
VERSION_NAME=$(grep "^app.version.name=" $GRADLE_PROPS | cut -d'=' -f2)
VERSION_CODE=$(grep "^app.version.code=" $GRADLE_PROPS | cut -d'=' -f2)

echo "Current version: $VERSION_NAME (code: $VERSION_CODE)"

# Parse version parts
IFS='.' read -r MAJOR MINOR PATCH <<< "$VERSION_NAME"

# Bump version
case "${1:-patch}" in
  major)
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
    ;;
  minor)
    MINOR=$((MINOR + 1))
    PATCH=0
    ;;
  patch|*)
    PATCH=$((PATCH + 1))
    ;;
esac

NEW_VERSION_NAME="$MAJOR.$MINOR.$PATCH"
NEW_VERSION_CODE=$((VERSION_CODE + 1))

echo "New version: $NEW_VERSION_NAME (code: $NEW_VERSION_CODE)"

# Update gradle.properties
sed -i "s/^app.version.name=.*/app.version.name=$NEW_VERSION_NAME/" $GRADLE_PROPS
sed -i "s/^app.version.code=.*/app.version.code=$NEW_VERSION_CODE/" $GRADLE_PROPS

echo "✓ Updated gradle.properties"
