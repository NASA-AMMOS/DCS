import pytest

import harness as h
from conftest import apply_process
from gov.nasa.jpl.ammos.kmc.sdlsclient import KmcSdlsClient as K

PDU = h.pattern(90)


def _cbc_frame(spec, pdu):
    return h.tm_aos_frame(spec, pdu, h.aos_header(spec.scid, spec.vcid), with_mac=False)


def _aead_frame(spec, pdu):
    return h.tm_aos_frame(spec, pdu, h.aos_header(spec.scid, spec.vcid))


@pytest.mark.parametrize("shivf_len,max_len,pdu_len", h.CBC_ROUNDTRIP_CASES)
def test_cbc_roundtrip(make_client, shivf_len, max_len, pdu_len):
    spec = h.aos_cbc_partial_iv(shivf_len=shivf_len, max_frame_length=max_len)
    pdu = h.pattern(pdu_len)
    c = make_client("aos", [spec])
    applied, res = apply_process(c, "aos", _cbc_frame(spec, pdu))
    data_field = max_len - 6 - spec.sec_header_len - 2
    assert len(applied) == max_len
    assert bytes(res.aos_pdu) == pdu
    assert res.aos_security_header.pad == data_field - pdu_len  # big-endian pad length


@pytest.mark.parametrize("shivf_len,max_len,pdu_len", h.CBC_MISMATCH_CASES)
def test_cbc_pad_frame_size_mismatch_rejected_at_apply(make_client, shivf_len, max_len, pdu_len):
    spec = h.aos_cbc_partial_iv(shivf_len=shivf_len, max_frame_length=max_len)
    c = make_client("aos", [spec])
    with pytest.raises(K.SdlsClientException) as e:
        c.apply_security_aos(_cbc_frame(spec, h.pattern(pdu_len)))
    assert e.value.error_code == h.CRYPTO_LIB_ERR_AOS_APPLY_PADDING


@pytest.mark.parametrize("check_fecf", [False, True])
def test_reported_fecf_equals_wire_fecf(make_client, check_fecf):
    spec = h.aos_gcm_partial_iv(name="aos_fecf", spi=49, vcid=2)
    pdu = h.pattern(h.aead_pdu_len(spec))
    c = make_client("aos", [spec], check_fecf=check_fecf)
    applied, res = apply_process(c, "aos", _aead_frame(spec, pdu))
    assert res.aos_security_trailer.fecf == int.from_bytes(applied[-2:], "big")


def test_reported_fecf_for_cbc_frame(make_client):
    spec = h.aos_cbc_partial_iv()
    c = make_client("aos", [spec], check_fecf=False)
    applied, res = apply_process(c, "aos", _cbc_frame(spec, PDU))
    assert res.aos_security_trailer.fecf == int.from_bytes(applied[-2:], "big")


@pytest.mark.requires_inmemory
def test_iv_rollover_on_process(make_client):
    spec = h.aos_gcm_partial_iv(name="aos_iv_rollover", spi=50, vcid=3, iv=bytes(4) + b"\xff" * 8, arsnw=5)
    pdu = h.pattern(h.aead_pdu_len(spec))
    c = make_client("aos", [spec], ignore_antireplay=False)
    sa = c.get_sa(spec.spi)
    start_iv = bytes(spec.iv)
    sa.iv = start_iv[:-1] + b"\xfe"
    frames = [c.apply_security_aos(_aead_frame(spec, pdu)) for _ in range(3)]
    sa.iv = start_iv[:-1] + b"\xfe"
    for frame, want_prefix in zip(frames[1:], (bytes(4), bytes(3) + b"\x01")):
        assert bytes(c.process_security_aos(bytearray(frame)).aos_pdu) == pdu
        assert c.get_sa(spec.spi).iv[:4] == want_prefix


def test_auth_only_hmac(make_client):
    """est=0, ast=1, ecs=AES-GCM, acs=HMAC-SHA256, no IV: MAC is written, verified, and tampering is rejected."""
    spec = h.auth_only_hmac("aos", "aos_auth_only_hmac", 51, 4)
    pdu = h.pattern(h.aead_pdu_len(spec))
    c = make_client("aos", [spec])
    frame = _aead_frame(spec, pdu)
    applied = c.apply_security_aos(bytearray(frame))
    assert bytes(applied[-18:-2]) != bytes(frame[-18:-2]), "apply did not write a MAC"
    assert bytes(c.process_security_aos(bytearray(applied)).aos_pdu) == pdu
    tampered = bytearray(applied)
    tampered[20] ^= 1
    with pytest.raises(K.SdlsClientException):
        c.process_security_aos(tampered)


def test_true_gmac_is_rejected_not_silently_accepted(make_client):
    """GMAC (est=0, ast=1, ecs=AES-GCM, acs=none, with IV) is unsupported. Older CryptoLib wrote no MAC and then
    accepted any frame on process; it must now fail at apply."""
    spec = h.gmac("aos", "aos_gmac", 53, 5)
    pdu = h.pattern(h.aead_pdu_len(spec))
    c = make_client("aos", [spec])
    with pytest.raises(K.SdlsClientException) as e:
        c.apply_security_aos(bytearray(_aead_frame(spec, pdu)))
    assert e.value.error_code == h.CRYPTO_LIB_ERR_UNSUPPORTED_ACS
