#!/bin/bash
# daily_commit.sh - ensures at least one commit per execution
# Appends a timestamp to daily_commits.log and commits it.
cd "$(dirname "$0")"
echo "$(date '+%Y-%m-%d %H:%M:%S')" >> daily_commits.log
git add daily_commits.log
git commit -m "Daily auto-commit $(date '+%H:%M:%S')"
