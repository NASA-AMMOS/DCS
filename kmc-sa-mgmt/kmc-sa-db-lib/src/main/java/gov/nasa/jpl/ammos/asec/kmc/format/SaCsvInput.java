package gov.nasa.jpl.ammos.asec.kmc.format;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnFactory;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ServiceType;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CSV Input
 */
public class SaCsvInput {
    private static final Logger LOG = LoggerFactory.getLogger(SaCsvInput.class);

    /**
     * Parse CSV from reader
     *
     * @param reader CSV input
     * @param type   frame type
     * @return list of SAs from CSV
     * @throws KmcException ex
     */
    public List<ISecAssn> parseCsv(Reader reader, FrameType type) throws KmcException {
        // come in with a desired frame type.
        if (type == FrameType.UNKNOWN) {
            return Collections.emptyList();
        }
        List<ISecAssn> sas = new ArrayList<>();
        try {
            Iterable<CSVRecord> records =
                    CSVFormat.Builder.create(CSVFormat.EXCEL).setHeader().setSkipHeaderRecord(false).build().parse(reader);
            for (CSVRecord rec : records) {
                // if the type column is mapped, then we only want to include those that match the desired type (or ALL)
                if (rec.isMapped("type")) {
                    FrameType recType = FrameType.fromString(rec.get("type"));
                    if (type == FrameType.ALL || recType == type) {
                        // convert if the desired type is ALL, or the recType == the desired type
                        sas.add(convertRecord(rec, recType));
                        // else, skip
                    } else if (recType == FrameType.UNKNOWN && LOG.isWarnEnabled()) {
                        LOG.warn("Unknown frame type encountered, skipping: {}", rec.get("type"));
                    }
                } else {
                    // unmapped, need to coerce it to desired type, default to TC
                    FrameType coerceTo = getCoerceTo(type);
                    sas.add(convertRecord(rec, coerceTo));
                }
            }
        } catch (Exception e) {
            throw new KmcException("Unable to parse CSV due to unexpected error", e);
        }
        return sas;
    }

    private static FrameType getCoerceTo(FrameType type) {
        FrameType coerceTo;
        if (type == FrameType.ALL) {
            coerceTo = FrameType.TC;
        } else {
            coerceTo = type;
        }
        return coerceTo;
    }

    /**
     * Convert a parsed CSV record to a Security Association object
     *
     * @param rec csv record
     * @param type   frame type
     * @return sa
     * @throws KmcException ex
     */
    public ISecAssn convertRecord(CSVRecord rec, FrameType type) throws KmcException {
        try {
            ISecAssn sa = SecAssnFactory.createSecAssn(type);
            sa.setSpi(parseInt(rec.get("spi")));
            sa.setScid(parseShort(rec.get("scid")));
            sa.setVcid(parseByte(rec.get("vcid")));
            sa.setTfvn(parseByte(rec.get("tfvn")));
            sa.setMapid(parseByte(rec.get("mapid")));
            sa.setSaState(parseShort(rec.get("sa_state")));
            ServiceType st;
            st = getServiceType(rec);
            if (st == null) return null;
            sa.setEst(st.getEncryptionType());
            sa.setAst(st.getAuthenticationType());
            sa.setShivfLen(parseShort(rec.get("shivf_len")));
            sa.setShsnfLen(parseShort(rec.get("shsnf_len")));
            sa.setShplfLen(parseShort(rec.get("shplf_len")));
            sa.setStmacfLen(parseShort(rec.get("stmacf_len")));
            Short  ecsLen = (short) 1;
            String ecsStr = rec.get("ecs");
            byte[] ecs    = checkNullValue(ecsStr) ? null : parseHex(ecsStr);
            sa.setEcs(ecsLen, ecs);
            String ekid = rec.get("ekid");
            sa.setEkid(checkNullValue(ekid) ? null : ekid);
            Short  ivLen = parseShort(rec.get("iv_len"));
            String ivStr = rec.get("iv");
            byte[] iv    = checkNullValue(ivStr) ? null : parseHex(ivStr);
            sa.setIv(ivLen, iv);
            String acsStr   = rec.get("acs");
            byte[] acsBytes = checkNullValue(acsStr) ? null : parseHex(acsStr);
            Short  acsLen   = (short) 1;
            sa.setAcs(acsLen, acsBytes);
            String akid = rec.get("akid");
            sa.setAkid(checkNullValue(akid) ? null : akid);
            sa.setAbmLen(parseInt(rec.get("abm_len")));
            sa.setAbm(parseHex(rec.get("abm")));
            sa.setArsnLen(parseShort(rec.get("arsn_len")));
            sa.setArsn(parseHex(rec.get("arsn")));
            Short arsnw = parseShort(rec.get("arsnw"));
            sa.setArsnw(arsnw == null ? 0 : arsnw);
            return sa;
        } catch (DecoderException e) {
            LOG.error("Error parsing a hex value on line {}, skipping", rec.getRecordNumber() + 1);
            return null;
        } catch (NumberFormatException e) {
            LOG.error("Error parsing a number value on line {}, skipping", rec.getRecordNumber() + 1);
            return null;
        }
    }

    private ServiceType getServiceType(CSVRecord rec) throws KmcException {
        ServiceType st;
        try {
            st = ServiceType.fromShort(parseShort(rec.get("st")));
            if (st == ServiceType.UNKNOWN) {
                LOG.warn("Unknown service type provided, skipping");
                return null;
            }
        } catch (Exception e) {
            // try to parse out text value
            LOG.warn("Encountered an error while attempting to parse 'service type' as short, " +
                    "will attempt to parse value as text: {}", e.getMessage());
            try {
                st = ServiceType.valueOf(rec.get("st"));
            } catch (Exception e1) {
                throw new KmcException("Unable to evaluate provided service type");
            }
        }
        return st;
    }

    /**
     * Check the null value of a string
     *
     * @param value string value
     * @return is null or not
     */
    public boolean checkNullValue(String value) {
        return value == null || value.trim().isEmpty() || value.trim().equalsIgnoreCase("null");
    }

    /**
     * Parse hex from input
     *
     * @param hex hex value
     * @return byte array
     * @throws DecoderException decoder ex
     */
    public byte[] parseHex(String hex) throws DecoderException {
        if (checkNullValue(hex)) {
            return new byte[]{};
        }
        String h = hex.trim();
        if (h.matches("^X'([0-9A-F]{2})*'")) {
            h = h.replaceFirst("^X", "").replace("'", "");
        } else if (h.matches("^0x([0-9A-F]{2})*")) {
            h = h.replaceFirst("^0x", "");
        }
        if (h.startsWith("0x")) {
            h = h.replaceFirst("^0x", "");
        }
        if (h.startsWith("X'")) {
            h = h.replaceFirst("^X'", "").replace("'", "");
        }
        if (h.isEmpty()) {
            return new byte[0];
        }
        return Hex.decodeHex(h);
    }

    /**
     * Parse a short value from input
     *
     * @param value short string
     * @return short value
     */
    public Short parseShort(String value) {
        if (checkNullValue(value)) {
            return null;
        }
        return Short.parseShort(value);
    }

    /**
     * Parse an int value from input
     *
     * @param value int string
     * @return int value
     */
    public Integer parseInt(String value) {
        if (checkNullValue(value)) {
            return null;
        }
        return Integer.parseInt(value);
    }

    /**
     * Parse a byte value from input
     *
     * @param value byte string
     * @return byte value
     */
    public Byte parseByte(String value) {
        if (checkNullValue(value)) {
            return null;
        }
        return Byte.parseByte(value);
    }
}
