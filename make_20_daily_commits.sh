#!/bin/bash
# make_20_daily_commits.sh - creates 20 incremental commits for the day
# Each commit appends a timestamp line to daily_commits.log.

LOG_FILE="backend/src/main/java/com/unlimsync/CommitMarker.java"

for i in $(seq 1 20); do
  echo "// $(date '+%Y-%m-%d %H:%M:%S') - auto commit $i" >> $LOG_FILE
  git add $LOG_FILE
  git commit -m "Daily auto commit $i - $(date '+%H:%M:%S')"
done
