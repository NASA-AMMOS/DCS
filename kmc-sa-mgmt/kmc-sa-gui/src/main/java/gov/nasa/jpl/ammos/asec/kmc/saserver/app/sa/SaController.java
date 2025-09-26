package gov.nasa.jpl.ammos.asec.kmc.saserver.app.sa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;
import gov.nasa.jpl.ammos.asec.kmc.api.json.ByteArrayDeserializer;
import gov.nasa.jpl.ammos.asec.kmc.api.json.ByteArraySerializer;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.FrameType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ISecAssn;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SecAssnValidator;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.ServiceType;
import gov.nasa.jpl.ammos.asec.kmc.api.sa.SpiScid;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IDbSession;
import gov.nasa.jpl.ammos.asec.kmc.api.sadb.IKmcDao;
import gov.nasa.jpl.ammos.asec.kmc.format.SaCsvInput;
import gov.nasa.jpl.ammos.asec.kmc.format.SaCsvOutput;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static gov.nasa.jpl.ammos.asec.kmc.sadb.KmcDao.*;

/**
 * SADB REST Controller
 */
@RestController
@RequestMapping("/api")
public class SaController {

    private static final Logger       LOG            = LoggerFactory.getLogger(SaController.class);
    public static final  String       STATUS_KEY     = "status";
    public static final  String       ERROR_STATUS   = "error";
    public static final  String       SUCCESS_STATUS = "success";
    public static final  String       MESSAGES_KEY   = "messages";
    private final        IKmcDao      dao;
    private static final ObjectMapper mapper         = new ObjectMapper();

    @Autowired
    public SaController(IKmcDao dao) {
        this.dao = dao;
    }

    /**
     * Convenience class for reset ARSN operation
     */
    public static class IdArsn implements Serializable {
        private SpiScid id;
        @JsonSerialize(using = ByteArraySerializer.class)
        @JsonDeserialize(using = ByteArrayDeserializer.class)
        private byte[]  arsn;
        private Short   arsnLen;
        private Short   arsnw;

        public SpiScid getId() {
            return id;
        }

        public void setId(SpiScid id) {
            this.id = id;
        }

        public byte[] getArsn() {
            return arsn;
        }

        public void setArsn(byte[] arsn) {
            this.arsn = arsn;
        }

        public Short getArsnLen() {
            return arsnLen;
        }

        public void setArsnLen(Short arsnLen) {
            this.arsnLen = arsnLen;
        }

        public Short getArsnw() {
            return arsnw;
        }

        public void setArsnw(Short arsnw) {
            this.arsnw = arsnw;
        }
    }

    /**
     * Convenience class for reset IV operation
     */
    public static class IdIv implements Serializable {
        private SpiScid id;
        @JsonSerialize(using = ByteArraySerializer.class)
        @JsonDeserialize(using = ByteArrayDeserializer.class)
        private byte[]  iv;
        private Short   ivLen;

        public SpiScid getId() {
            return id;
        }

        public void setId(SpiScid id) {
            this.id = id;
        }

        public byte[] getIv() {
            return iv;
        }

        public void setIv(byte[] iv) {
            this.iv = iv;
        }

        public Short getIvLen() {
            return ivLen;
        }

        public void setIvLen(Short ivLen) {
            this.ivLen = ivLen;
        }
    }

    /**
     * Convenience class for rekey operation
     */
    public static class Rekey implements Serializable {
        private SpiScid id;
        private String  ekid;
        private String  akid;

        public SpiScid getId() {
            return id;
        }

        public void setId(SpiScid id) {
            this.id = id;
        }

        public String getEkid() {
            return ekid;
        }

        public void setEkid(String ekid) {
            this.ekid = ekid;
        }

        public String getAkid() {
            return akid;
        }

        public void setAkid(String akid) {
            this.akid = akid;
        }
    }

    @GetMapping({"/sa/{type}", "/sa"})
    public List<ISecAssn> getSa(@PathVariable(name = "type", required = false) String type,
                                @RequestParam(name = "scid", required = false) Short scid,
                                @RequestParam(name = "spi", required = false) Integer spi,
                                HttpServletRequest request) throws KmcException {
        FrameType frameType = StringUtils.hasText(type) ? FrameType.fromString(type) : FrameType.ALL;
        if (frameType == FrameType.UNKNOWN) {
            throw new KmcException(String.format("%s is an unknown frame type", type));
        }
        if (spi != null && scid != null) {
            if (frameType == FrameType.ALL) {
                throw new KmcException("must provide frame type when specifying SPI and SCID");
            }
            return Collections.singletonList(dao.getSa(new SpiScid(spi, scid), frameType));
        }
        LOG.info("{} retrieving {} SAs", request.getRemoteAddr(), frameType.name());
        List<ISecAssn> sas = dao.getSas(frameType);
        if (scid != null) {
            sas = sas.stream().filter(sa -> sa.getScid().equals(scid)).toList();
        }
        if (spi != null) {
            sas = sas.stream().filter(sa -> sa.getSpi().equals(spi)).toList();
        }
        LOG.info("{} sent {} {} SAs", request.getRemoteAddr(), sas.size(), frameType.name());
        return sas;
    }

    @PutMapping("/sa")
    public ISecAssn putSa(@RequestBody ISecAssn sa, HttpServletRequest request) throws KmcException {
        LOG.info("{} creating SA ({}/{})", request.getRemoteAddr(), sa.getSpi(), sa.getScid());
        ISecAssn newSa = dao.createSa(sa);
        LOG.info("{} created SA ({}/{})", request.getRemoteAddr(), sa.getSpi(), sa.getScid());
        return newSa;
    }

    @PostMapping(value = {"/sa/{type}", "/sa"}, consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ISecAssn postSa(@RequestBody ISecAssn sa, @PathVariable(name = "type", required = false) String type,
                           HttpServletRequest request) throws KmcException {
        FrameType frameType = StringUtils.hasText(type) ? FrameType.fromString(type) : FrameType.TC;
        if (frameType == FrameType.UNKNOWN) {
            throw new KmcException(String.format("%s is an unknown frame type", type));
        }
        LOG.info("{} updating {} SA ({}/{})", request.getRemoteAddr(), frameType.name(), sa.getSpi(), sa.getScid());
        checkEncryption(sa);
        checkAuthentication(sa);
        ISecAssn original = dao.getSa(sa.getId(), sa.getType());
        try (IDbSession dbSession = dao.newSession()) {
            dbSession.beginTransaction();
            if (!Objects.equals(original.getSaState(), sa.getSaState())) {
                switch (sa.getSaState()) {
                    case SA_OPERATIONAL:
                        dao.startSa(dbSession, sa.getId(), true, sa.getType());
                        break;
                    case SA_KEYED:
                        dao.stopSa(dbSession, sa.getId(), sa.getType());
                        break;
                    default: // handles SA_UNKEYED and SA_EXPIRED
                        dao.expireSa(dbSession, sa.getId(), sa.getType());
                        break;
                }
            }
            dao.updateSa(dbSession, sa);
            dbSession.commit();
        } catch (KmcException e) {
            throw e;
        } catch (Exception e) {
            throw new KmcException(e);
        }
        LOG.info("{} updated SA ({}/{})", request.getRemoteAddr(), sa.getSpi(), sa.getScid());
        return dao.getSa(sa.getId(), sa.getType());
    }

    private void checkAuthentication(ISecAssn sa) throws KmcException {
        if (sa.getServiceType() == ServiceType.AUTHENTICATION) {
            if (sa.getAcs() == null) {
                throw new KmcException("When service type is  AUTHENTICATION, AKID and ACS are" + " required");
            }
            int acs = 0;
            for (byte b : sa.getAcs()) {
                acs = (acs << 8) + (b & 0xff);
            }
            if ((sa.getSaState() != SA_EXPIRE || sa.getSaState() != SA_UNKEYED) && (acs == 0 || (sa.getAkid() == null || sa.getAkid().isEmpty()))) {
                throw new KmcException("When service type is  AUTHENTICATION, AKID and ACS are" + " required");
            }
        }
    }

    private void checkEncryption(ISecAssn sa) throws KmcException {
        if (sa.getServiceType() == ServiceType.ENCRYPTION || sa.getServiceType() == ServiceType.AUTHENTICATED_ENCRYPTION) {
            if (sa.getEcs() == null) {
                throw new KmcException("When service type is ENCRYPTION or AUTHENTICATED_ENCRYPTION, EKID and ECS " +
                        "are required");
            }
            int ecs = 0;
            for (byte b : sa.getEcs()) {
                ecs = (ecs << 8) + (b & 0xff);
            }
            if ((sa.getSaState() != SA_EXPIRE || sa.getSaState() != SA_UNKEYED) && (ecs == 0 || (sa.getEkid() == null || sa.getEkid().isEmpty()))) {
                throw new KmcException("When service type is ENCRYPTION or AUTHENTICATED_ENCRYPTION, EKID and ECS " +
                        "are required");
            }
        }
    }

    @PostMapping(value = {"/sa/start/{type}", "/sa/start"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ISecAssn startSa(@PathVariable(name = "type", required = false) String type, @RequestBody SpiScid id,
                            HttpServletRequest request) throws KmcException {
        FrameType frameType = StringUtils.hasText(type) ? FrameType.fromString(type) : FrameType.TC;
        LOG.info("{} starting SA ({}/{}) {}", request.getRemoteAddr(), id.getSpi(), id.getScid(), frameType.name());
        ISecAssn sa = dao.startSa(id, true, frameType);
        LOG.info("{} started SA ({}/{}) {}", request.getRemoteAddr(), id.getSpi(), id.getScid(), frameType.name());
        return sa;
    }

    @PostMapping(value = {"/sa/stop/{type}", "/sa/stop"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ISecAssn stopSa(@PathVariable(name = "type", required = false) String type, @RequestBody SpiScid id,
                           HttpServletRequest request) throws KmcException {
        FrameType frameType = StringUtils.hasText(type) ? FrameType.fromString(type) : FrameType.TC;
        LOG.info("{} stopping SA ({}/{}) {}", request.getRemoteAddr(), id.getSpi(), id.getScid(), frameType.name());
        ISecAssn sa = dao.stopSa(id, frameType);
        LOG.info("{} stopped SA ({}/{}) {}", request.getRemoteAddr(), id.getSpi(), id.getScid(), frameType.name());
        return sa;
    }

    @PostMapping(value = {"/sa/expire/{type}", "/sa/expire"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ISecAssn expireSa(@PathVariable(name = "type", required = false) String type, @RequestBody SpiScid id,
                             HttpServletRequest request) throws KmcException {
        FrameType frameType = StringUtils.hasText(type) ? FrameType.fromString(type) : FrameType.TC;
        LOG.info("{} expiring SA ({}/{}) {}", request.getRemoteAddr(), id.getSpi(), id.getScid(), frameType.name());
        ISecAssn sa = dao.expireSa(id, frameType);
        LOG.info("{} expired SA ({}/{}) {}", request.getRemoteAddr(), id.getSpi(), id.getScid(), frameType.name());
        return sa;
    }

    @PostMapping(value = {"/sa/arsn/{type}", "/sa/arsn"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> resetArsn(@PathVariable(name = "type", required = false) String type,
                                              @RequestBody IdArsn idArsn
            , HttpServletRequest request) throws KmcException {
        FrameType frameType = type == null ? FrameType.TC : FrameType.fromString(type);
        LOG.info("{} resetting ARSN on SA ({}/{}) {}", request.getRemoteAddr(), idArsn.id.getSpi(),
                idArsn.id.getScid(), frameType.name());
        ObjectNode respBody = mapper.createObjectNode();
        ISecAssn   sa       = dao.getSa(idArsn.id, frameType);
        if (sa != null) {
            try {
                if (idArsn.arsn.length < idArsn.arsnLen) {
                    int    diff    = idArsn.arsnLen - idArsn.arsn.length;
                    byte[] newArsn = new byte[idArsn.arsnLen];
                    System.arraycopy(idArsn.arsn, 0, newArsn, diff, idArsn.arsn.length);
                    idArsn.arsn = newArsn;
                    respBody.withArray(MESSAGES_KEY).add("Array left padded with " + diff + " bytes");
                } else if (idArsn.arsn.length > idArsn.arsnLen) {
                    respBody.put(STATUS_KEY, ERROR_STATUS);
                    respBody.withArray(MESSAGES_KEY).add("ARSN is larger than ARSN length in bytes");
                    return ResponseEntity.badRequest().body(respBody);
                }
                sa.setArsn(idArsn.arsn);
                sa.setArsnLen(idArsn.arsnLen);
                sa.setArsnw(idArsn.arsnw);
                dao.updateSa(sa);
                respBody.put(STATUS_KEY, SUCCESS_STATUS);
                LOG.info("{} reset ARSN on SA ({}/{}) {}", request.getRemoteAddr(), idArsn.id.getSpi(),
                        idArsn.id.getScid(), frameType.name());
                return ResponseEntity.ok().body(respBody);
            } catch (IllegalArgumentException e) {
                respBody.put(STATUS_KEY, ERROR_STATUS);
                respBody.withArray(MESSAGES_KEY).add("ARSN input not a valid hex string");
            }
        }
        respBody.put(STATUS_KEY, ERROR_STATUS);
        respBody.withArray(MESSAGES_KEY).add("An unknown error occurred");
        LOG.info("{} failed to reset ARSN on SA ({}/{}) {}", request.getRemoteAddr(), idArsn.id.getSpi(),
                idArsn.id.getScid(), frameType.name());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respBody);
    }

    @DeleteMapping({"/sa/{type}", "/sa"})
    public void deleteSa(@PathVariable(name = "type", required = false) String type, @RequestBody List<SpiScid> ids,
                         HttpServletRequest request) throws KmcException {
        FrameType frameType = type == null ? FrameType.TC : FrameType.fromString(type);
        for (SpiScid id : ids) {
            LOG.info("{} deleting SA ({}/{}) {}", request.getRemoteAddr(), id.getSpi(), id.getScid(), frameType.name());
            dao.deleteSa(id, frameType);
            LOG.info("{} deleted SA ({}/{}) {}", request.getRemoteAddr(), id.getSpi(), id.getScid(), frameType.name());
        }
    }

    @PostMapping(value = {"/sa/create/{type}", "/sa/create"})
    public ResponseEntity<ObjectNode> bulkCreate(@PathVariable(name = "type", required = false) List<String> types,
                                                 @RequestParam("file") MultipartFile file,
                                                 @RequestParam(name = "force", defaultValue = "false") boolean force,
                                                 HttpServletRequest request) throws IOException,
                                                                                    KmcException {
        Set<FrameType> frameTypes = new HashSet<>();
        if (types != null) {
            for (String type : types) {
                FrameType frameType = FrameType.fromString(type);
                if (frameType != FrameType.UNKNOWN) {
                    frameTypes.add(frameType);
                }
            }
        } else {
            frameTypes.add(FrameType.TC);
        }

        LOG.info("{} creating SAs from file", request.getRemoteAddr());
        ObjectNode resp   = mapper.createObjectNode();
        boolean    errors = false;
        SaCsvInput input  = new SaCsvInput();
        int        count  = 0;
        int        errs   = 0;
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            List<ISecAssn> sas = input.parseCsv(reader, FrameType.ALL);
            sas = sas.stream().filter(sa -> frameTypes.contains(sa.getType())).toList();
            try (IDbSession session = dao.newSession()) {
                for (ISecAssn sa : sas) {
                    session.beginTransaction();
                    try {
                        SecAssnValidator.validate(sa);
                        ISecAssn exists = dao.getSa(sa.getId(), sa.getType());
                        if (exists != null && force) {
                            dao.deleteSa(session, sa.getId(), sa.getType());
                            session.flush();
                        }
                        dao.createSa(session, sa);
                        count++;
                    } catch (KmcException e) {
                        errors = true;
                        errs++;
                        resp.withArray(MESSAGES_KEY).add(e.getMessage());
                        session.rollback();
                    } finally {
                        if (session.isActive()) {
                            session.commit();
                        }
                    }
                }
            } catch (Exception e) {
                handleException(e);
            }
        }
        HttpStatus status = HttpStatus.CREATED;
        if (!errors) {
            resp.put(STATUS_KEY, SUCCESS_STATUS);
            LOG.info("{} created {} SAs from file", request.getRemoteAddr(), count);
        } else {
            status = HttpStatus.BAD_REQUEST;
            resp.put(STATUS_KEY, ERROR_STATUS);
            LOG.info("{} failed to create SAs from file with {} errors", request.getRemoteAddr(), errs);
        }
        return new ResponseEntity<>(resp, status);
    }

    @GetMapping(value = {"/sa/csv/{type}", "/sa/csv"})
    public ResponseEntity<String> downloadCsv(@PathVariable(name = "type", required = false) String type,
                                              HttpServletRequest request) throws KmcException {
        FrameType frameType = StringUtils.hasText(type) ? FrameType.fromString(type) : FrameType.ALL;
        LOG.info("{} downloading {} SAs as CSV", request.getRemoteAddr(), frameType.name());
        SaCsvOutput    out = new SaCsvOutput(true);
        List<ISecAssn> sas = dao.getSas(frameType);
        StringWriter   w   = new StringWriter();
        try (PrintWriter pw = new PrintWriter(w)) {
            out.print(pw, sas);
        }
        SimpleDateFormat sdf      = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String           fileName = "SADB_" + sdf.format(new Date()) + ".csv";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName).header("X-Suggested-Filename", fileName).body(w.toString());
    }

    @PostMapping(value = {"/sa/iv/{type}", "/sa/iv"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> resetIv(@PathVariable(name = "type", required = false) String type,
                                            @RequestBody IdIv idIv,
                                            HttpServletRequest request) {
        FrameType  frameType = StringUtils.hasText(type) ? FrameType.fromString(type) : FrameType.TC;
        ObjectNode respBody  = mapper.createObjectNode();
        LOG.info("{} resetting IV on ({}/{}) {}", request.getRemoteAddr(), idIv.id.getSpi(), idIv.id.getScid(),
                frameType.name());
        try (IDbSession dbSession = dao.newSession()) {
            try {
                dbSession.beginTransaction();
                ISecAssn sa = dao.getSa(dbSession, idIv.id, frameType);
                if (sa != null) {
                    try {
                        if (idIv.iv.length < idIv.ivLen) {
                            int    diff  = idIv.ivLen - idIv.iv.length;
                            byte[] newIv = new byte[idIv.ivLen];
                            System.arraycopy(idIv.iv, 0, newIv, diff, idIv.iv.length);
                            idIv.iv = newIv;
                            respBody.withArray(MESSAGES_KEY).add("Array left padded with " + diff + " bytes");
                        } else if (idIv.iv.length > idIv.ivLen) {
                            respBody.put(STATUS_KEY, ERROR_STATUS);
                            respBody.withArray(MESSAGES_KEY).add("IV is larger than IV length in bytes");
                            return ResponseEntity.badRequest().body(respBody);
                        }
                        sa.setIv(idIv.iv);
                        sa.setIvLen(idIv.ivLen);
                        dao.updateSa(dbSession, sa);
                        respBody.put(STATUS_KEY, SUCCESS_STATUS);
                        LOG.info("{} reset IV on ({}/{}) {}", request.getRemoteAddr(), idIv.id.getSpi(),
                                idIv.id.getScid(),
                                frameType.name());
                        return ResponseEntity.ok().body(respBody);
                    } catch (IllegalArgumentException e) {
                        respBody.put(STATUS_KEY, ERROR_STATUS);
                        respBody.withArray(MESSAGES_KEY).add("IV input not a valid Base64 string");
                    }
                }
            } finally {
                dbSession.commit();
            }
        } catch (Exception e) {
            handleException(e);
        }

        respBody.put(STATUS_KEY, ERROR_STATUS);
        respBody.withArray(MESSAGES_KEY).add("An unknown error occurred");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respBody);
    }

    @PostMapping(value = {"/sa/key/{type}", "/sa/key"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> rekeySa(@PathVariable(name = "type", required = false) String type,
                                            @RequestBody Rekey rekey,
                                            HttpServletRequest request) {
        FrameType frameType = StringUtils.hasText(type) ? FrameType.fromString(type) : FrameType.TC;
        LOG.info("{} Rekeying SA ({}/{}) {}", request.getRemoteAddr(), rekey.id.getSpi(), rekey.id.getScid(),
                frameType.name());
        try (IDbSession dbSession = dao.newSession()) {
            try {
                dbSession.beginTransaction();
                ISecAssn sa = dao.getSa(dbSession, rekey.id, frameType);
                if (rekey.ekid != null && !rekey.ekid.equals(sa.getEkid())) {
                    dao.rekeySaEnc(dbSession, sa.getId(), rekey.ekid, sa.getEcs(), sa.getEcsLen(), frameType);
                    dbSession.flush();
                }
                if (rekey.akid != null && !rekey.akid.equals(sa.getAkid())) {
                    dao.rekeySaAuth(dbSession, sa.getId(), rekey.akid, sa.getAcs(), sa.getAcsLen(), frameType);
                    dbSession.flush();
                }
            } finally {
                dbSession.commit();
            }
            LOG.info("{} Rekeyed SA ({}/{}) {}", request.getRemoteAddr(), rekey.id.getSpi(), rekey.id.getScid(),
                    frameType.name());
        } catch (Exception e) {
            handleException(e);
        }

        return ResponseEntity.ok().body(mapper.createObjectNode().put(STATUS_KEY, SUCCESS_STATUS));
    }

    @GetMapping(value = "/status")
    public ResponseEntity<JsonNode> status() {
        boolean    status = dao.status();
        ObjectNode node   = mapper.createObjectNode();
        node.put(STATUS_KEY, status ? "ok" : "database down");
        return new ResponseEntity<>(node, status ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE);
    }

    @GetMapping(value = "/health")
    public String health() {
        // This method will simply return HTTP 200 status with the following string
        return "Service is UP\n";
    }

    @ExceptionHandler({KmcException.class, Exception.class})
    public ResponseEntity<Object> handleException(Exception e) {
        LOG.error("An exception occurred", e);
        ObjectNode node = mapper.createObjectNode();
        node.put(STATUS_KEY, ERROR_STATUS);
        node.withArray(MESSAGES_KEY).add(e.getMessage());
        return new ResponseEntity<>(node, HttpStatus.BAD_REQUEST);
    }
}
