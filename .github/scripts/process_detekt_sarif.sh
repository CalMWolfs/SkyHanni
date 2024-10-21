#!/bin/bash

# This script processes the Detekt SARIF file and outputs results in a format
# suitable for annotation in CI/CD systems, with debug output.

SARIF_FILE="$1"

# Check if SARIF file exists
if [ ! -f "$SARIF_FILE" ]; then
    echo "SARIF file not found: $SARIF_FILE"
    exit 1
fi

# Print the raw SARIF file content for debugging
#echo "==== RAW SARIF FILE CONTENT ===="
#cat "$SARIF_FILE"
#echo "==============================="

# Define jq command to parse SARIF file
read -r -d '' jq_command <<'EOF'
.runs[].results[] |
{
    "full_path": .locations[].physicalLocation.artifactLocation.uri | sub("file://$(pwd)/"; ""),
    "file_name": (.locations[].physicalLocation.artifactLocation.uri | split("/") | last),
    "l": .locations[].physicalLocation,
    "level": .level,
    "message": .message.text,
    "ruleId": .ruleId
} |
(
    ":no:" + (.level) +
    " file=" + (.full_path) +
    ",line=" + (.l.region.startLine|tostring) +
    ",title=" + (.ruleId) +
    ",col=" + (.l.region.startColumn|tostring) +
    ",endColumn=" + (.l.region.endColumn|tostring) +
    "::" + (.message)
)
EOF

# Run jq command to format the output, and print debug info of parsed SARIF data
echo "==== FORMATTED OUTPUT FROM JQ ===="
jq -r "$jq_command" < "$SARIF_FILE"
echo "==============================="
