/**
 * KMIPStub.java
 * -----------------------------------------------------------------
 *     __ __ __  ___________
 *    / //_//  |/  /  _/ __ \	  .--.
 *   / ,<  / /|_/ // // /_/ /	 /.-. '----------.
 *  / /| |/ /  / // // ____/ 	 \'-' .--"--""-"-'
 * /_/ |_/_/  /_/___/_/      	  '--'
 *
 * -----------------------------------------------------------------
 * Description:
 * The Stub encapsulates the whole KMIP functionality of the
 * client side. To process a request, it encodes the request,
 * sends it to the server over the transport layer, and finally
 * decodes and returns the response.
 *
 * @author     Stefanie Meile <stefaniemeile@gmail.com>
 * @author     Michael Guster <michael.guster@gmail.com>
 * @org.       NTB - University of Applied Sciences Buchs, (CH)
 * @copyright  Copyright ��� 2013, Stefanie Meile, Michael Guster
 * @license    Simplified BSD License (see LICENSE.TXT)
 * @version    1.0, 2013/08/09
 * @since      Class available since Release 1.0
 *
 *
 */

package ch.ntb.inf.kmip.stub;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ntb.inf.kmip.container.KMIPContainer;
import ch.ntb.inf.kmip.process.decoder.KMIPDecoderInterface;
import ch.ntb.inf.kmip.process.encoder.KMIPEncoderInterface;
import ch.ntb.inf.kmip.stub.transport.KMIPStubTransportLayerInterface;
import ch.ntb.inf.kmip.test.UCStringCompare;
import ch.ntb.inf.kmip.utils.KMIPUtils;

/**
 * The KMIPStubInterface is the interface for all stubs. It
 * provides the needful flexibility for the interchangeability of
 * the stub.
 * The Stub encapsulates the whole KMIP functionality of the
 * server side. To process a request, it offers two superimposed
 * methods:
 * <ul>
 * 	<li><code>processRequest(KMIPContainer c)</code> for common use</li>
 * 	<li><code>processRequest(KMIPContainer c, String expectedTTLVRequest, String expectedTTLVResponse)</code> for test cases</li>
 * </ul>
 */
public class KMIPStub implements KMIPStubInterface {

    private static final String DEFAULT_ENCODER = "ch.ntb.inf.kmip.process.encoder.KMIPEncoder";
    private static final String DEFAULT_DECODER = "ch.ntb.inf.kmip.process.decoder.KMIPDecoder";
    private static final String DEFAULT_TLS_TRANSPORT = "ch.ntb.inf.kmip.stub.transport.KMIPStubTransportLayer";
    private static final String DEFAULT_HTTP_TRANSPORT = "ch.ntb.inf.kmip.stub.transport.KMIPStubTransportLayerHTTP";

    // transport layer config parameters
    public static final String CFG_KMS_URI = "key_management_service_uri";
    public static final String CFG_KEYSTORE_FILE = "keystore_file";
    public static final String CFG_KEYSTORE_PASSWORD = "keystore_password";
    public static final String CFG_SSO_COOKIE = "sso_cookie";
    public static final String CFG_SOCKET_PORT = "socket_port";
    public static final String CFG_SOCKET_PROTOCOLS = "socket_protocols";
    public static final String CFG_SOCKET_CIPHERS = "socket_ciphers";

	private static final Logger logger = LoggerFactory.getLogger(KMIPStub.class);

	private KMIPEncoderInterface encoder;
	private KMIPDecoderInterface decoder;
	private KMIPStubTransportLayerInterface transportLayer;

	/**
	 *
	 * @param configParams The hashmap contains parameters for the transport layer.
	 * @throws Exception If a config parameter is not valid.
	 */
	public KMIPStub(final Map<String, Object> configParams) throws Exception {
		try {
		    this.encoder = (KMIPEncoderInterface) Class.forName(DEFAULT_ENCODER).getDeclaredConstructor().newInstance();
		    this.decoder = (KMIPDecoderInterface) Class.forName(DEFAULT_DECODER).getDeclaredConstructor().newInstance();
		    String kmsURI = (String) configParams.get(CFG_KMS_URI);
		    logger.debug("KMIPStub: kmsURI = " + kmsURI);
		    if (kmsURI.startsWith("http")) {
		        this.transportLayer = (KMIPStubTransportLayerInterface) Class.forName(DEFAULT_HTTP_TRANSPORT).getDeclaredConstructor().newInstance();
		        this.transportLayer.setConfigParameters(configParams);
		    } else if (kmsURI.startsWith("tls10://") || kmsURI.startsWith("tls12://")) {
		        this.transportLayer = (KMIPStubTransportLayerInterface) Class.forName(DEFAULT_TLS_TRANSPORT).getDeclaredConstructor().newInstance();
		        this.transportLayer.setConfigParameters(configParams);
		    } else {
		        String msg = "Invalid " + CFG_KMS_URI + ": " + kmsURI;
		        logger.error(msg);
		        throw new IllegalArgumentException(msg);
		    }
		    logger.debug("transport = " + transportLayer.getClass());

		    //UCStringCompare.testingOption = props.getIntProperty("Testing");
		    UCStringCompare.testingOption = 0;
		} catch (Exception e) {
			logger.error("KMIPStub(): " + e);
			throw e;
		}
	}

	/**
	 * Processes a KMIP-Request-Message stored in a <code>KMIPContainer</code> and returns a corresponding KMIP-Response-Message.
	 *
	 * @param c :      	the <code>KMIPContainer</code> to be encoded and sent.
	 * @return			<code>KMIPContainer</code> with the response objects.
	 */
	@Override
    public KMIPContainer processRequest(final KMIPContainer c)
		throws Exception {
		ArrayList<Byte> ttlv = encoder.encodeRequest(c);
		ArrayList<Byte> responseFromServer = transportLayer.send(ttlv);
		if (responseFromServer == null) {
		    return null;
		} else {
		    return decodeResponse(responseFromServer);
		}
	}

	/**
	 * Processes a KMIP-Request-Message stored in a <code>KMIPContainer</code> and returns a corresponding KMIP-Response-Message.
	 * For test cases, there are two additional parameters that may be set by the caller. The idea is, that the generated TTLV-Strings
	 * can be compared to the expected TTLV-Strings.
	 *
	 * @param c :      	the <code>KMIPContainer</code> to be encoded and sent.
	 * @param expectedTTLVRequest :      	the <code>String</code> to be compared to the encoded request message.
	 * @param expectedTTLVResponse :      	the <code>String</code> to be compared to the decoded response message.
	 * @return			<code>KMIPContainer</code> with the response objects.
	 */
	@Override
    public KMIPContainer processRequest(final KMIPContainer c, final String expectedTTLVRequest, final String expectedTTLVResponse)
		throws Exception {
		// encode Request
		ArrayList<Byte> ttlv = encoder.encodeRequest(c);
		logger.info("Encoded Request from Client: (actual/expected)");
		KMIPUtils.printArrayListAsHexString(ttlv);
		logger.debug(expectedTTLVRequest);
		UCStringCompare.checkRequest(ttlv, expectedTTLVRequest);

		// send Request and check Response
		ArrayList<Byte> responseFromServer = transportLayer.send(ttlv);
		if (responseFromServer == null) {
		    return null;
		}
		logger.info("Encoded Response from Server: (actual/expected)");
		KMIPUtils.printArrayListAsHexString(responseFromServer);
		logger.debug(expectedTTLVResponse);
		UCStringCompare.checkResponse(responseFromServer,expectedTTLVResponse);
		return decodeResponse(responseFromServer);
	}

	private KMIPContainer decodeResponse(final ArrayList<Byte> responseFromServer)
		throws Exception {
		try {
			return decoder.decodeResponse(responseFromServer);
		} catch (Exception e) {
			logger.error("decodeResponse(): " + e);
			// comment out as it prints garbage
			//logger.error("responseFromServer: " + byteListToString(responseFromServer));
			throw e;
		}
	}

	// not working (test by comment out the sso cookie)
	public static String byteListToString(final List<Byte> l) {
	    if (l == null) {
	        return "";
	    }
	    byte[] array = new byte[l.size()];
	    int i = 0;
	    for (Byte current : l) {
	        array[i] = current;
	        i++;
	    }
	    try {
            return new String(array, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
	}

}
