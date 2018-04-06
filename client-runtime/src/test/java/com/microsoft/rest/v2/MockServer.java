package com.microsoft.rest.v2;


import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.ResourceHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class MockServer {
    private static class TestHandler extends HandlerWrapper {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
            byte[] buf = new byte[8192];
            InputStream is = request.getInputStream();
            MessageDigest md5;
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            while (true) {
                int bytesRead = is.read(buf);
                if (bytesRead == -1) {
                    break;
                }
                md5.update(buf, 0, bytesRead);
            }

            byte[] md5Digest = md5.digest();
            String encodedMD5 = Base64.getEncoder().encodeToString(md5Digest);
            response.setStatus(201);
            response.setHeader("Content-MD5", encodedMD5);
            baseRequest.setHandled(true);
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(11081);
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setResourceBase("client-runtime/temp/");
        ContextHandler ch = new ContextHandler("/javasdktest/upload");
        ch.setHandler(resourceHandler);

        HandlerList handlers = new HandlerList();
        handlers.addHandler(ch);
        handlers.addHandler(new TestHandler());

        server.setHandler(handlers);

        System.out.println("Starting MockServer");
        server.start();
        server.join();
        System.out.println("Shutting down MockServer");
    }
}
