package gov.nasa.jpl.ammos.asec.kmc.kmcsdlsservice;

import gov.nasa.jpl.ammos.asec.kmc.KmcSdlsEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.util.Properties;

public final class KmcSdlsService
{
    private static KmcSdlsService INSTANCE = null;
    private static Logger LOG = LoggerFactory.getLogger(KmcSdlsService.class);
    private KmcSdlsEngine kmcSdlsEngine = null;
    private KmcSdlsServiceConfiguration kmcConfig = null;

    private KmcSdlsService(){
    }

    private void initKmcConfiguration()
    {
        if(INSTANCE.kmcConfig != null) {
            return; // case where synchronized access after previous access which already inited the KmcSdlsEngine
        }
        INSTANCE.kmcConfig = new KmcSdlsServiceConfiguration();
        Properties props = INSTANCE.kmcConfig.getConfiguration();
        try
        {
            INSTANCE.kmcSdlsEngine = new KmcSdlsEngine(props);
            INSTANCE.LOG.info("KMC SDLS Service Initialized Successfully.");
        } catch (Exception e)
        {
            INSTANCE.LOG.error("KMC SDLS Service Initialization Failure!");
            INSTANCE.LOG.error(e.getMessage());
        }
    }

    public static synchronized KmcSdlsService getInstance(){
        if(INSTANCE == null){
            INSTANCE = new KmcSdlsService();
        }
        return INSTANCE;
    }

    // Initialization must happen here otherwise the Spring Environment hasn't had time to start up and load necessary properties.
    @PostConstruct
    public static KmcSdlsEngine getKmcSdlsEngine(){
        if(INSTANCE.kmcSdlsEngine == null){
            synchronized (KmcSdlsService.class) {
                INSTANCE.initKmcConfiguration();
            }
        }
        return INSTANCE.kmcSdlsEngine;
    }

}

