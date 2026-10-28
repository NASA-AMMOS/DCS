import pytest

import harness as h
from conftest import apply_process

# 1-byte pad field: PKCS pad = 16 - (len % 16), a full block when the PDU is already block aligned
PDU_LENS = [1, 2, 14, 15, 16, 17, 31, 32]


def _frame(spec, pdu):
    total = 5 + len(pdu) + 2
    return bytearray(h.tc_header(spec.scid, spec.vcid, total) + pdu + b"\xe3\x16")


@pytest.mark.parametrize("n", PDU_LENS)
def test_cbc_one_byte_pad_field(make_client, n):
    spec = h.tc_cbc(1, 48)
    pdu = h.pattern(n, 0xA0)
    c = make_client("tc", [spec])
    applied, res = apply_process(c, "tc", _frame(spec, pdu))
    assert bytes(res.tc_pdu) == pdu
    expected_pad = 16 - (n % 16)
    assert applied[5 + 2 + spec.shivf_len] == expected_pad  # wire pad byte after header, SPI and IV
    assert len(applied) == 5 + spec.sec_header_len + n + expected_pad + 2


def test_cbc_two_byte_pad_field(make_client):
    spec = h.tc_cbc(2, 49)
    pdu = h.pattern(2, 0x01)
    c = make_client("tc", [spec])
    applied, res = apply_process(c, "tc", _frame(spec, pdu))
    assert bytes(res.tc_pdu) == pdu
    assert applied[5 + 2 + spec.shivf_len:5 + 2 + spec.shivf_len + 2] == b"\x00\x0e"  # pad 14, big-endian
