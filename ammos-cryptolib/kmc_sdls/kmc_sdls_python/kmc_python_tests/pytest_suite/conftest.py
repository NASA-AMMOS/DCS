"""
Pytest suite for the SDLS Python client against CryptoLib.

  pytest                       # default: --backend=gcrypt (in-memory SADB + libgcrypt + CryptoLib internal keys)
  pytest --backend=kmc --kmc-props=my.properties
                               # MariaDB SADB (pre-seeded with kmc_seed.sql) + KMC crypto service

The gcrypt backend needs a native build with -DKEY_INTERNAL=ON; otherwise those tests are skipped.
"""
import pytest

from gov.nasa.jpl.ammos.kmc.sdlsclient import KmcSdlsClient as K
from harness import managed_props


def pytest_addoption(parser):
    parser.addoption("--backend", choices=("gcrypt", "kmc"), default="gcrypt",
                     help="gcrypt: hermetic (default). kmc: MariaDB SADB + KMC crypto service")
    parser.addoption("--kmc-props", default=None,
                     help="properties file with cryptolib.sadb.mariadb.* and cryptolib.crypto.kmccryptoservice.* "
                          "(required for --backend=kmc)")


def pytest_collection_modifyitems(config, items):
    for item in items:
        if "upstream_bug" in item.keywords:
            item.add_marker(pytest.mark.xfail(reason="known CryptoLib defect", strict=False))
    if config.getoption("--backend") == "kmc":
        skip = pytest.mark.skip(reason="needs direct SA access (in-memory SADB); not available with --backend=kmc")
        for item in items:
            if "requires_inmemory" in item.keywords:
                item.add_marker(skip)


@pytest.fixture(scope="session")
def backend(request):
    return request.config.getoption("--backend")


def _kmc_props(request):
    path = request.config.getoption("--kmc-props")
    if not path:
        pytest.fail("--backend=kmc requires --kmc-props=<file>")
    with open(path) as f:
        return [l.strip() for l in f if l.strip() and not l.lstrip().startswith("#")]


@pytest.fixture
def make_client(request, backend):
    """
    Factory: make_client(frame_type, specs, ignore_antireplay=True, check_fecf=False) -> client.
    On the gcrypt backend it also installs each SaSpec into the in-memory SADB. All clients are shut down and the
    fixture SAs disabled on teardown (CryptoLib state is process-global, so use one client at a time).
    """
    created = []

    def factory(frame_type, specs, ignore_antireplay=True, check_fecf=False):
        t = frame_type
        if backend == "gcrypt":
            base = ["cryptolib.sadb.type=inmemory", "cryptolib.crypto.type=libgcrypt", "cryptolib.key.type=internal"]
        else:
            base = _kmc_props(request)
        props = base + ["cryptolib.%s.vcid_bitmask=0x3f" % t, "cryptolib.%s.create_ecf=true" % t,
                        "cryptolib.%s.check_fecf=%s" % (t, str(check_fecf).lower()),
                        "cryptolib.%s.ignore_antireplay=%s" % (t, str(ignore_antireplay).lower()),
                        "cryptolib.%s.on_rollover_increment_nontransmitted_counter=true" % t]
        if t == "tc":
            props += ["cryptolib.tc.ignore_sa_state=true", "cryptolib.tc.process_pdus=false"]
        for s in specs:
            props += managed_props(s)
        try:
            client = K.KmcSdlsClient(props)
        except K.SdlsClientException as e:
            if backend == "gcrypt" and "KEY_INTERNAL" in str(e):
                pytest.skip("native build lacks internal keys (rebuild with -DKEY_INTERNAL=ON)")
            raise
        created.append((client, specs))
        if backend == "gcrypt":
            for s in specs:
                s.apply_to(client.get_sa(s.spi))
        return client

    yield factory
    for client, specs in created:
        if backend == "gcrypt":
            for s in specs:
                client.get_sa(s.spi).sa_state = 0
        client.shutdown()


def apply_process(client, frame_type, frame):
    """Apply then process; returns (applied bytearray, processed frame object)."""
    apply_fn = getattr(client, "apply_security_" + frame_type)
    process_fn = getattr(client, "process_security_" + frame_type)
    applied = apply_fn(frame)
    return applied, process_fn(bytearray(applied))
