package com.tme.security;

import static org.junit.Assert.*;
import java.nio.file.*;
import org.apache.catalina.*;
import org.apache.catalina.connector.*;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.valves.*;
import org.junit.Test;

public class DeploymentConfigurationTest {
    @Test public void runtimeXmlLoadsPortAndProxySettings() throws Exception {
        String[] keys = {"catalina.base", "catalina.home", "http.port", "proxy.remoteHeader", "proxy.protocolHeader"};
        String[] previous = new String[keys.length];
        for (int i = 0; i < keys.length; i++) previous[i] = System.getProperty(keys[i]);
        Catalina catalina = new Catalina();
        try {
            Path base = Path.of("target/deployment-config").toAbsolutePath();
            Files.createDirectories(base.resolve("conf"));
            Files.copy(Path.of("deploy/tomcat/server.xml"), base.resolve("conf/server.xml"), StandardCopyOption.REPLACE_EXISTING);
            System.setProperty("catalina.base", base.toString());
            System.setProperty("catalina.home", base.toString());
            System.setProperty("http.port", "0");
            System.setProperty("proxy.remoteHeader", "CF-Connecting-IP");
            System.setProperty("proxy.protocolHeader", "X-Forwarded-Proto");
            catalina.setConfigFile(base.resolve("conf/server.xml").toString());
            catalina.setUseNaming(false);
            catalina.load();
            Server server = catalina.getServer();
            assertNotNull(server);
            assertEquals(LifecycleState.INITIALIZED, server.getState());
            assertEquals(-1, server.getPort());
            Service service = server.findServices()[0];
            Connector connector = service.findConnectors()[0];
            assertEquals(0, connector.getPort());
            assertTrue(connector.getProperty("address").toString().contains("0.0.0.0"));
            Host host = (Host) service.getContainer().findChild("localhost");
            assertFalse(host.getAutoDeploy());
            RemoteIpValve remote = null;
            for (Valve valve : host.getPipeline().getValves()) if (valve instanceof RemoteIpValve) remote = (RemoteIpValve) valve;
            assertNotNull(remote);
            assertEquals("CF-Connecting-IP", remote.getRemoteIpHeader());
            assertEquals("X-Forwarded-Proto", remote.getProtocolHeader());
        } finally {
            if (catalina.getServer() != null) catalina.getServer().destroy();
            for (int i = 0; i < keys.length; i++) {
                if (previous[i] == null) System.clearProperty(keys[i]); else System.setProperty(keys[i], previous[i]);
            }
        }
    }

    @Test public void forwardedHeadersRequireTrustedPeerAndIgnoreForgedForwardedFor() throws Exception {
        assertProxy("10.1.2.3", "198.51.100.5", true);
        assertProxy("203.0.113.10", "203.0.113.10", false);
    }

    private void assertProxy(String peer, String expectedAddress, boolean expectedSecure) throws Exception {
        Connector connector = new Connector();
        Request request = new Request(connector);
        org.apache.coyote.Request coyote = new org.apache.coyote.Request();
        coyote.remoteAddr().setString(peer);
        coyote.remoteHost().setString(peer);
        coyote.scheme().setString("http");
        coyote.serverName().setString("localhost");
        coyote.setServerPort(10000);
        coyote.getMimeHeaders().setValue("CF-Connecting-IP").setString("198.51.100.5");
        coyote.getMimeHeaders().setValue("X-Forwarded-For").setString("192.0.2.99");
        coyote.getMimeHeaders().setValue("X-Forwarded-Proto").setString("https");
        request.setCoyoteRequest(coyote);
        Response response = new Response();
        request.setResponse(response);
        response.setRequest(request);
        RemoteIpValve valve = new RemoteIpValve();
        valve.setRemoteIpHeader("CF-Connecting-IP");
        valve.setProtocolHeader("X-Forwarded-Proto");
        valve.setNext(new ValveBase() {
            @Override public void invoke(Request req, Response resp) {
                assertEquals(expectedAddress, req.getRemoteAddr());
                assertEquals(expectedSecure, req.isSecure());
            }
        });
        valve.invoke(request, response);
    }
}
