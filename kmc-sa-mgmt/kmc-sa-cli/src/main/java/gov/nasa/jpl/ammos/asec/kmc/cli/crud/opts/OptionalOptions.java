package gov.nasa.jpl.ammos.asec.kmc.cli.crud.opts;

import picocli.CommandLine;

/**
 * Optional opts
 */
@CommandLine.Command
public class OptionalOptions {
    /**
     * EKID opt
     */
    @CommandLine.Option(names = "--ekid", required = false, description = "encryption key ID")
    public String ekid;

    /**
     * ECS opt
     */
    @CommandLine.Option(names = "--ecs", required = false, description = "encryption cypher suite (hex string)")
    public String ecs;

    /**
     * AKID opt
     */
    @CommandLine.Option(names = "--akid", required = false, description = "authentication key ID")
    public String akid;

    /**
     * ACS opt
     */
    @CommandLine.Option(names = "--acs", required = false, description = "authentication cypher suite")
    public String acs;

    /**
     * IV opt
     */
    @CommandLine.Option(names = "--iv", required = false, description = "initialization vector (hex string)")
    public String iv;

    /**
     * IV length opt
     */
    @CommandLine.Option(names = "--ivlen", required = false, description = "initialization vector length, in bytes")
    public Short ivLen;

    /**
     * ARSN opt
     */
    @CommandLine.Option(names = "--arsn", required = false, description =
            "anti-replay sequence number starting " + "value (hex string)")
    public String arsn;

    /**
     * ARSN length opt
     */
    @CommandLine.Option(names = "--arsnlen", required = false, description = "anti-replay sequence number length "
            + "(bytes)")
    public Short arsnlen;

    /**
     * ARSNW opt
     */
    @CommandLine.Option(names = "--arsnw", required = false, description = "anti-replay sequence number window")
    public Short arsnw;

    /**
     * ABM opt
     */
    @CommandLine.Option(names = "--abm", required = false, description = "authentication bitmask (hex string)")
    public String abm;

    /**
     * ABM length opt
     */
    @CommandLine.Option(names = "--abmlen", required = false, description = "authentication bitmask length (bytes)")
    public Integer abmLen;

    /**
     * SHIVF length opt
     */
    @CommandLine.Option(names = "--shivflen", required = false, description =
            "security header initialization " + "vector field length (bytes)")
    public Short shivfLen;

    /**
     * SHSNF length opt
     */
    @CommandLine.Option(names = "--shsnflen", required = false, description =
            "security header sequence number " + "field length (bytes)")
    public Short shsnfLen;

    /**
     * SHPLF length opt
     */
    @CommandLine.Option(names = "--shplflen", required = false, description =
            "security header pad length field " + "length (bytes)")
    public Short shplfLen;

    /**
     * ST MACF length opt
     */
    @CommandLine.Option(names = "--stmacflen", required = false, description = "security trailer MAC field " +
            "length" + " (bytes)")
    public Short stmacfLen;

    /**
     * Service type opt
     */
    @CommandLine.Option(names = "--st", required = false, description = "service type. 0 = PLAINTEXT, 1 = " +
            "ENCRYPTION, 2 = AUTHENTICATION, 3 = AUTHENTICATED_ENCRYPTION")
    public String st;
}
