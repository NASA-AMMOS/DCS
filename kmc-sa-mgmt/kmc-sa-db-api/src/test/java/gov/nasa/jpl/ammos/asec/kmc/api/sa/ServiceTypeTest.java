package gov.nasa.jpl.ammos.asec.kmc.api.sa;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ServiceTypeTest {
    @Test
    public void getServiceTypeFromEstAst() {
        assertEquals(ServiceType.PLAINTEXT, ServiceType.getServiceType((short) 0, (short) 0));
        assertEquals(ServiceType.ENCRYPTION, ServiceType.getServiceType((short) 1, (short) 0));
        assertEquals(ServiceType.AUTHENTICATION, ServiceType.getServiceType((short) 0, (short) 1));
        assertEquals(ServiceType.AUTHENTICATED_ENCRYPTION, ServiceType.getServiceType((short) 1, (short) 1));
    }

    @Test
    public void getServiceTypeNullIsUnknown() {
        assertEquals(ServiceType.UNKNOWN, ServiceType.getServiceType(null, (short) 0));
        assertEquals(ServiceType.UNKNOWN, ServiceType.getServiceType((short) 0, null));
        assertEquals(ServiceType.UNKNOWN, ServiceType.getServiceType(null, null));
    }
}
