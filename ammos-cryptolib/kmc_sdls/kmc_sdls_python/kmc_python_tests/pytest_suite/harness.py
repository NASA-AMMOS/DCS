"""Frame builders, SA specs and client helpers shared by the SDLS pytest suite."""
from dataclasses import dataclass, field
from typing import Optional

ECS_NONE, ECS_GCM, ECS_CBC = 0, 1, 2
ACS_NONE, ACS_CMAC, ACS_HMAC_SHA256 = 0, 1, 2
SA_OPERATIONAL = 3
KEY_128 = 128  # CryptoLib internal key ring: 32-byte, KEY_ACTIVE

CRYPTO_LIB_ERR_TM_APPLY_PADDING = -90
CRYPTO_LIB_ERR_AOS_APPLY_PADDING = -92
CRYPTO_LIB_ERR_UNSUPPORTED_ACS = -26

TEST_SCID = 200  # TM/AOS fixtures; also the SCID used by kmc_seed.sql
TC_SCID = 99


@dataclass
class SaSpec:
    """One fixture SA. spi must be unused by CryptoLib's built-in defaults (0-15)."""
    name: str
    frame_type: str  # tc | tm | aos
    spi: int
    scid: int
    vcid: int
    tfvn: int
    est: int = 1
    ast: int = 0
    ecs: int = ECS_GCM
    acs: int = 0
    iv_len: int = 12
    shivf_len: int = 12
    shsnf_len: int = 0
    shplf_len: int = 0
    stmacf_len: int = 0
    arsn_len: int = 0
    arsnw: int = 0
    iv: bytes = b""
    arsn: bytes = b""
    ekid: int = KEY_128
    akid: int = 0
    max_frame_length: int = 128
    has_ocf: bool = False
    extra: dict = field(default_factory=dict)

    @property
    def sec_header_len(self):
        return 2 + self.shivf_len + self.shsnf_len + self.shplf_len

    def apply_to(self, handle):
        handle.sa_state = SA_OPERATIONAL
        for f in ("tfvn", "scid", "vcid", "est", "ast", "ecs", "acs", "iv_len", "shivf_len", "shsnf_len",
                  "shplf_len", "stmacf_len", "arsn_len", "arsnw", "ekid", "akid"):
            setattr(handle, f, getattr(self, f))
        handle.mapid = 0
        handle.ecs_len = 1
        handle.acs_len = 1
        handle.null_iv = 0
        handle.null_arsn = 0
        handle.iv = self.iv
        handle.arsn = self.arsn
        if self.ast:
            handle.abm_len = 150
            handle.abm = b"\xff" * 150
        else:
            handle.abm_len = 0


def pattern(n, start=1):
    return bytes((start + i) & 0xFF for i in range(n))


def tm_header(scid, vcid, ocf=False):
    word = (0 << 14) | (scid << 4) | (vcid << 1) | int(ocf)
    return word.to_bytes(2, "big") + bytes(4)


def aos_header(scid, vcid):
    word = (1 << 14) | (scid << 6) | vcid
    return word.to_bytes(2, "big") + bytes(4)


def tc_header(scid, vcid, total_len):
    b01 = (0b00 << 6) | (1 << 5) | scid >> 8
    return bytes([b01, scid & 0xFF, (vcid << 2) | ((total_len - 1) >> 8), (total_len - 1) & 0xFF, 0])


def tm_aos_frame(spec, pdu, header, with_mac=True, ocf=False):
    """header + zeroed security header + PDU + zeroed MAC + (OCF) + zeroed FECF."""
    return bytearray(header + bytes(spec.sec_header_len) + pdu + bytes(spec.stmacf_len if with_mac else 0)
                     + (b"\xde\xad\xbe\xef" if ocf else b"") + bytes(2))


def aead_pdu_len(spec, ocf=False):
    """PDU length that makes an AEAD frame exactly max_frame_length."""
    return spec.max_frame_length - 6 - spec.sec_header_len - spec.stmacf_len - (4 if ocf else 0) - 2


def managed_props(spec):
    k = "cryptolib.%s.%d.%d.%d." % (spec.frame_type, spec.scid, spec.vcid, spec.tfvn)
    props = [k + "has_ecf=true", k + "max_frame_length=%d" % spec.max_frame_length]
    if spec.frame_type == "tc":
        props += [k + "has_segmentation_header=false", k + "has_pus_header=false"]
    elif spec.frame_type == "tm":
        props += [k + "has_ocf=%s" % str(spec.has_ocf).lower()]
    else:
        props += [k + "has_ocf=%s" % str(spec.has_ocf).lower(), k + "has_iz=false", k + "has_fhec=false",
                  k + "iz_len=0"]
    return props


# ---- fixture SAs (kmc_seed.sql mirrors these) -------------------------------------------------------------------

def tm_cbc_partial_iv(ocf=False, shivf_len=12, max_frame_length=None):
    full_iv = shivf_len == 16
    return SaSpec("tm_cbc_full_iv" if full_iv else "tm_cbc_partial_iv", "tm", 54 if full_iv else 41, TEST_SCID,
                  6 if full_iv else 0, 0, ecs=ECS_CBC, iv_len=16, shivf_len=shivf_len,
                  shplf_len=2, iv=bytes(15) + b"\x01", max_frame_length=max_frame_length or (124 if ocf else 120),
                  has_ocf=ocf)


def tm_gcm_partial_iv(**kw):
    d = dict(ast=1, stmacf_len=16, iv_len=12, shivf_len=8, iv=bytes(11) + b"\x01", ecs=ECS_GCM)
    d.update(kw)
    return SaSpec(d.pop("name", "tm_gcm_partial_iv"), "tm", d.pop("spi", 42), TEST_SCID, d.pop("vcid", 1), 0, **d)


def aos_cbc_partial_iv(shivf_len=12, max_frame_length=120):
    full_iv = shivf_len == 16
    return SaSpec("aos_cbc_full_iv" if full_iv else "aos_cbc_partial_iv", "aos", 55 if full_iv else 43, TEST_SCID,
                  6 if full_iv else 0, 1, ecs=ECS_CBC, iv_len=16, shivf_len=shivf_len,
                  shplf_len=2, iv=bytes(15) + b"\x01", max_frame_length=max_frame_length)


def aos_gcm_partial_iv(**kw):
    d = dict(ast=1, stmacf_len=16, iv_len=12, shivf_len=8, iv=bytes(11) + b"\x01", ecs=ECS_GCM)
    d.update(kw)
    return SaSpec(d.pop("name", "aos_gcm_partial_iv"), "aos", d.pop("spi", 44), TEST_SCID, d.pop("vcid", 1), 1, **d)


def auth_only_hmac(frame_type, name, spi, vcid):
    """est=0, ast=1, ecs=AES-GCM, acs=HMAC-SHA256, no IV: CryptoLib's 'auth-only AEAD' SA (MAC over header + PDU)."""
    cls = tm_gcm_partial_iv if frame_type == "tm" else aos_gcm_partial_iv
    return cls(name=name, spi=spi, vcid=vcid, est=0, ast=1, acs=ACS_HMAC_SHA256, iv_len=0, shivf_len=0, iv=b"",
               akid=KEY_128, extra={"kmc_akid": "kmc/test/HmacSHA256"})


def gmac(frame_type, name, spi, vcid):
    """True GMAC: est=0, ast=1, ecs=AES-GCM, acs=none, with an IV. CryptoLib does not support this."""
    cls = tm_gcm_partial_iv if frame_type == "tm" else aos_gcm_partial_iv
    return cls(name=name, spi=spi, vcid=vcid, est=0, akid=KEY_128)


def tc_cbc(shplf_len, vcid):
    return SaSpec("tc_cbc_shplf%d" % shplf_len, "tc", 60 + shplf_len, TC_SCID, vcid, 0, ecs=ECS_CBC, iv_len=16,
                  shivf_len=12, shplf_len=shplf_len, iv=bytes(15) + b"\x01", max_frame_length=1024)


# CBC frame sizes: (shivf_len, max_frame_length, pdu_len). The data field (max - 6 header - security header - 2 FECF)
# must be a multiple of 16 and PDU + pad must fill it. Includes full-IV SAs and the 1020/1772-byte robot-style sizes.
CBC_ROUNDTRIP_CASES = [(12, 120, 90), (16, 124, 86), (16, 1020, 982), (12, 1768, 1736)]
# Frames whose PKCS pad cannot fill the fixed-length data field must be rejected at apply (robot AOS VCID 1 was one).
CBC_MISMATCH_CASES = [(12, 128, 90), (12, 1772, 1740), (16, 1024, 988)]


# ---- pre-seeded SADB for --backend=kmc ----------------------------------------------------------------------------

def kmc_seeded_specs():
    """SAs that kmc_seed.sql must contain for the tests that do not need in-memory SA access."""
    return [tm_cbc_partial_iv(), tm_cbc_partial_iv(shivf_len=16), tm_gcm_partial_iv(),
            auth_only_hmac("tm", "tm_auth_only_hmac", 48, 4), gmac("tm", "tm_gmac", 52, 5),
            aos_cbc_partial_iv(), aos_cbc_partial_iv(shivf_len=16), aos_gcm_partial_iv(),
            aos_gcm_partial_iv(name="aos_fecf", spi=49, vcid=2),
            auth_only_hmac("aos", "aos_auth_only_hmac", 51, 4), gmac("aos", "aos_gmac", 53, 5),
            tc_cbc(1, 48), tc_cbc(2, 49)]


KMC_SPI_BASE = 9000  # SPIs in the SADB; the in-memory SADB only has 64 slots so specs use small SPIs
_TABLES = {"tc": "security_associations", "tm": "security_associations_tm", "aos": "security_associations_aos"}


def seed_sql():
    lines = ["-- Generated by `python harness.py > kmc_seed.sql`; do not edit by hand.",
             "-- SPIs 9000+; TM/AOS SCID 200 and TC SCID 99 VCIDs 48/49, which are disjoint from the robot SADB rows.",
             "-- Cleanup: DELETE FROM <table> WHERE spi BETWEEN 9000 AND 9099;", ""]
    for s in kmc_seeded_specs():
        t = _TABLES[s.frame_type]
        ek = "'kmc/test/KEY128'" if s.est else "NULL"
        ak = ("'%s'" % s.extra.get("kmc_akid", "kmc/test/KEY128")) if (s.ast and not s.est) else "NULL"
        abm_len = 150 if s.ast else 0
        lines.append("DELETE FROM %s WHERE spi=%d;" % (t, KMC_SPI_BASE + s.spi))
        lines.append(
            "INSERT INTO %s (spi,ekid,akid,sa_state,tfvn,scid,vcid,mapid,est,ast,shivf_len,shsnf_len,shplf_len,"
            "stmacf_len,ecs_len,ecs,iv_len,iv,acs_len,acs,abm_len,abm,arsn_len,arsn,arsnw) VALUES "
            "(%d,%s,%s,3,%d,%d,%d,0,%d,%d,%d,%d,%d,%d,1,x'%02x',%d,x'%s',1,x'%02x',%d,x'%s',%d,x'%s',%d); -- %s"
            % (t, KMC_SPI_BASE + s.spi, ek, ak, s.tfvn, s.scid, s.vcid, s.est, s.ast, s.shivf_len, s.shsnf_len, s.shplf_len,
               s.stmacf_len, s.ecs, s.iv_len, s.iv.hex(), s.acs, abm_len, (b"\xff" * abm_len).hex(), s.arsn_len,
               s.arsn.hex(), s.arsnw, s.name))
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    print(seed_sql(), end="")
