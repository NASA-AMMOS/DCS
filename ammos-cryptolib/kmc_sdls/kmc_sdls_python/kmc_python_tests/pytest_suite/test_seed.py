import os

import harness as h


def test_kmc_seed_sql_is_in_sync():
    path = os.path.join(os.path.dirname(__file__), "kmc_seed.sql")
    with open(path) as f:
        assert f.read() == h.seed_sql(), "run: python harness.py > kmc_seed.sql"
