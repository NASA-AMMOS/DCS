import pytest

import harness as h
from conftest import apply_process
from gov.nasa.jpl.ammos.kmc.sdlsclient import KmcSdlsClient as K

PDU = h.pattern(90)


def _frame(spec, pdu, ocf=False):
    return h.tm_aos_frame(spec, pdu, h.tm_header(spec.scid, spec.vcid, ocf), with_mac=False, ocf=ocf)


@pytest.mark.parametrize("shivf_len,max_len,pdu_len", h.CBC_ROUNDTRIP_CASES)
def test_cbc_roundtrip(make_client, shivf_len, max_len, pdu_len):
    spec = h.tm_cbc_partial_iv(shivf_len=shivf_len, max_frame_length=max_len)
    pdu = h.pattern(pdu_len)
    c = make_client("tm", [spec])
    applied, res = apply_process(c, "tm", _frame(spec, pdu))
    data_field = max_len - 6 - spec.sec_header_len - 2
    assert len(applied) == max_len
    assert bytes(res.tm_pdu) == pdu
    assert res.tm_security_header.pad == data_field - pdu_len  # big-endian pad length


def test_cbc_with_ocf_roundtrip(make_client):
    spec = h.tm_cbc_partial_iv(ocf=True)
    c = make_client("tm", [spec])
    _, res = apply_process(c, "tm", _frame(spec, PDU, ocf=True))
    assert bytes(res.tm_pdu) == PDU


@pytest.mark.parametrize("shivf_len,max_len,pdu_len", h.CBC_MISMATCH_CASES)
def test_cbc_pad_frame_size_mismatch_rejected_at_apply(make_client, shivf_len, max_len, pdu_len):
    spec = h.tm_cbc_partial_iv(shivf_len=shivf_len, max_frame_length=max_len)
    c = make_client("tm", [spec])
    with pytest.raises(K.SdlsClientException) as e:
        c.apply_security_tm(_frame(spec, h.pattern(pdu_len)))
    assert e.value.error_code == h.CRYPTO_LIB_ERR_TM_APPLY_PADDING


def test_gcm_partial_iv_roundtrip(make_client):
    spec = h.tm_gcm_partial_iv()
    pdu = h.pattern(h.aead_pdu_len(spec))
    c = make_client("tm", [spec])
    _, res = apply_process(c, "tm", h.tm_aos_frame(spec, pdu, h.tm_header(spec.scid, spec.vcid)))
    assert bytes(res.tm_pdu) == pdu


@pytest.mark.requires_inmemory
def test_iv_rollover_on_process(make_client):
    """Sender rolls the transmitted IV over; receiver's non-transmitted prefix must follow (separate receiver)."""
    spec = h.tm_gcm_partial_iv(name="tm_iv_rollover", spi=46, vcid=2, iv=bytes(4) + b"\xff" * 8, arsnw=5)
    pdu = h.pattern(h.aead_pdu_len(spec))
    c = make_client("tm", [spec], ignore_antireplay=False)
    sa = c.get_sa(spec.spi)
    start_iv = bytes(spec.iv)
    sa.iv = start_iv[:-1] + b"\xfe"  # last transmitted IV = ...fe
    frames = [c.apply_security_tm(h.tm_aos_frame(spec, pdu, h.tm_header(spec.scid, spec.vcid))) for _ in range(3)]
    sa.iv = start_iv[:-1] + b"\xfe"  # receiver: last seen ...fe
    for frame, want_prefix in zip(frames[1:], (bytes(4), bytes(3) + b"\x01")):  # ...ff, then rollover
        res = c.process_security_tm(bytearray(frame))
        assert bytes(res.tm_pdu) == pdu
        assert c.get_sa(spec.spi).iv[:4] == want_prefix


@pytest.mark.requires_inmemory
def test_arsn_rollover_on_process(make_client):
    spec = h.tm_gcm_partial_iv(name="tm_arsn_rollover", spi=47, vcid=3, shivf_len=12, shsnf_len=2, arsn_len=4,
                               arsnw=5, iv=bytes(11) + b"\x01", arsn=b"\x00\x00\xff\xfe")
    pdu = h.pattern(h.aead_pdu_len(spec))
    c = make_client("tm", [spec], ignore_antireplay=False)
    sa = c.get_sa(spec.spi)
    frames = [c.apply_security_tm(h.tm_aos_frame(spec, pdu, h.tm_header(spec.scid, spec.vcid))) for _ in range(3)]
    sa.iv = bytes(12)
    sa.arsn = b"\x00\x00\xff\xfd"
    for frame in frames:  # SN ...fffe, ...ffff, 0000 (prefix must roll to 0001)
        assert bytes(c.process_security_tm(bytearray(frame)).tm_pdu) == pdu
    assert c.get_sa(spec.spi).arsn[:4] == b"\x00\x01\x00\x00"


def test_auth_only_hmac(make_client):
    """est=0, ast=1, ecs=AES-GCM, acs=HMAC-SHA256, no IV: MAC is written, verified, and tampering is rejected."""
    spec = h.auth_only_hmac("tm", "tm_auth_only_hmac", 48, 4)
    pdu = h.pattern(h.aead_pdu_len(spec))
    c = make_client("tm", [spec])
    frame = h.tm_aos_frame(spec, pdu, h.tm_header(spec.scid, spec.vcid))
    applied = c.apply_security_tm(bytearray(frame))
    assert bytes(applied[-18:-2]) != bytes(frame[-18:-2]), "apply did not write a MAC"
    assert bytes(c.process_security_tm(bytearray(applied)).tm_pdu) == pdu
    tampered = bytearray(applied)
    tampered[20] ^= 1
    with pytest.raises(K.SdlsClientException):
        c.process_security_tm(tampered)


def test_true_gmac_is_rejected_not_silently_accepted(make_client):
    """GMAC (est=0, ast=1, ecs=AES-GCM, acs=none, with IV) is unsupported. Older CryptoLib wrote no MAC and then
    accepted any frame on process; it must now fail at apply."""
    spec = h.gmac("tm", "tm_gmac", 52, 5)
    pdu = h.pattern(h.aead_pdu_len(spec))
    c = make_client("tm", [spec])
    with pytest.raises(K.SdlsClientException) as e:
        c.apply_security_tm(bytearray(h.tm_aos_frame(spec, pdu, h.tm_header(spec.scid, spec.vcid))))
    assert e.value.error_code == h.CRYPTO_LIB_ERR_UNSUPPORTED_ACS
