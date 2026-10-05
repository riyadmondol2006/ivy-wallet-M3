"""Validate an accounts-table migration SQL and preserved rows in an in-memory SQLite database.

Usage: verify_dual_currency_migration.py [FROM TO MIGRATION_FILE]
Defaults to the 132 -> 133 credit-card due-date migration; pass 131 132 Migration131to132_DualCurrencyCards.kt
to check the dual-currency one.

Host-side check only; this does not replace Room's on-device migration validation.
"""
import json
import re
import sqlite3
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[1]
schema_dir = root / "shared/data/core/schemas/com.ivy.data.db.IvyRoomDatabase"
from_version, to_version, migration_file = (sys.argv[1:4] if len(sys.argv) == 4 else ("132", "133", "Migration132to133_CreditCardDueDates.kt"))
before = json.loads((schema_dir / f"{from_version}.json").read_text())["database"]
after = json.loads((schema_dir / f"{to_version}.json").read_text())["database"]
migration = (root / "shared/data/core/src/main/java/com/ivy/data/db/migration" / migration_file).read_text()
db = sqlite3.connect(":memory:")
for entity in before["entities"]:
    db.execute(entity["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
before_account_columns = {f["columnName"] for e in before["entities"] if e["tableName"] == "accounts" for f in e["fields"]}
db.execute("""INSERT INTO accounts(name,currency,color,orderNum,includeInBalance,creditLimit,isSynced,isDeleted,id)
              VALUES ('Visa','BDT',1,0,0,100000,0,0,'card')""")
db.execute("""INSERT INTO transactions(accountId,type,amount,isSynced,isDeleted,id)
              VALUES ('card','EXPENSE',20000,0,0,'expense')""")
for sql in re.findall(r'db.execSQL\("([^"]+)"\)', migration):
    db.execute(sql)
assert db.execute("SELECT name,creditLimit FROM accounts").fetchone() == ("Visa", 100000)
for added in {f["columnName"] for e in after["entities"] if e["tableName"] == "accounts" for f in e["fields"]} - before_account_columns:
    assert db.execute(f"SELECT {added} FROM accounts").fetchone()[0] in (None, 0), added
assert db.execute("SELECT accountId,amount FROM transactions").fetchone() == ("card", 20000)
columns = {row[1]: row for row in db.execute("PRAGMA table_info(accounts)")}
expected = next(entity for entity in after["entities"] if entity["tableName"] == "accounts")
assert set(columns) == {field["columnName"] for field in expected["fields"]}
for field in expected["fields"]:
    actual = columns[field["columnName"]]
    assert actual[2] == field["affinity"]
    assert bool(actual[3]) == field.get("notNull", False)
print(f"PASS: schema {from_version} -> {to_version} matches generated account columns and preserves card and transaction rows")
