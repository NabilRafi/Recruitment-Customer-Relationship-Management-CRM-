#!/bin/bash
# Removes application rows left behind by an earlier test run - rows whose
# candidate account was never registered. Safe to run at any time; it only
# deletes rows that reference a non-existent account.
cd "$(dirname "$0")"
echo "Orphaned application rows found:"
sqlite3 data/crm.db "SELECT id, candidate_email FROM applications
  WHERE candidate_email NOT IN (SELECT email FROM accounts);"
sqlite3 data/crm.db "DELETE FROM applications
  WHERE candidate_email NOT IN (SELECT email FROM accounts);"
echo "Removed. Remaining applications:"
sqlite3 data/crm.db "SELECT COUNT(*) FROM applications;"
