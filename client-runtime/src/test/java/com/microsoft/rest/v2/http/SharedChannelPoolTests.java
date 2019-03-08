package com.microsoft.rest.v2.http;

import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class SharedChannelPoolTests {
    @Test
    public void testGetChannelRequestPortWithNoPortInFtpURI() throws URISyntaxException {
        final URI uri = new URI("ftp://example.com/path/to/file.html");
        assertEquals(80, SharedChannelPool.getChannelRequestPort(uri));
    }

    @Test
    public void testGetChannelRequestPortWithNoPortInHttpURI() throws URISyntaxException {
        final URI uri = new URI("http://example.com/path/to/file.html");
        assertEquals(80, SharedChannelPool.getChannelRequestPort(uri));
    }

    @Test
    public void testGetChannelRequestPortWithNoPortInHttpsURI() throws URISyntaxException {
        final URI uri = new URI("https://example.com/path/to/file.html");
        assertEquals(443, SharedChannelPool.getChannelRequestPort(uri));
    }

    @Test
    public void testGetChannelRequestPortWithPortInFtpURI() throws URISyntaxException {
        final URI uri = new URI("ftp://example.com:1/path/to/file.html");
        assertEquals(1, SharedChannelPool.getChannelRequestPort(uri));
    }

    @Test
    public void testGetChannelRequestPortWithPortInHttpURI() throws URISyntaxException {
        final URI uri = new URI("http://example.com:2/path/to/file.html");
        assertEquals(2, SharedChannelPool.getChannelRequestPort(uri));
    }

    @Test
    public void testGetChannelRequestPortWithPortInHttpsURI() throws URISyntaxException {
        final URI uri = new URI("https://example.com:3/path/to/file.html");
        assertEquals(3, SharedChannelPool.getChannelRequestPort(uri));
    }

    @Test
    public void testGetChannelRequestDestinationURIWithSchemeAndHost() throws URISyntaxException {
        final URI uri = new URI("http://example.com");
        final int port = 4;
        final URI expectedDestinationUri = new URI("http://example.com:4");
        assertEquals(expectedDestinationUri, SharedChannelPool.getChannelRequestDestinationURI(uri, port));
    }

    @Test
    public void testGetChannelRequestDestinationURIWithSchemeHostAndPort() throws URISyntaxException {
        final URI uri = new URI("http://example.com:5");
        final int port = 6;
        final URI expectedDestinationUri = new URI("http://example.com:6");
        assertEquals(expectedDestinationUri, SharedChannelPool.getChannelRequestDestinationURI(uri, port));
    }

    @Test
    public void testGetChannelRequestDestinationURIWithSchemeHostAndPath() throws URISyntaxException {
        final URI uri = new URI("http://example.com/path.html");
        final int port = 7;
        final URI expectedDestinationUri = new URI("http://example.com:7");
        assertEquals(expectedDestinationUri, SharedChannelPool.getChannelRequestDestinationURI(uri, port));
    }

    @Test
    public void testGetChannelRequestDestinationURIWithSchemeHostPortAndPath() throws URISyntaxException {
        final URI uri = new URI("http://example.com:8/path.html");
        final int port = 9;
        final URI expectedDestinationUri = new URI("http://example.com:9");
        assertEquals(expectedDestinationUri, SharedChannelPool.getChannelRequestDestinationURI(uri, port));
    }

    @Test
    public void testGetChannelRequestChannelURIWithNoProxyAndNoDestinationUri() throws URISyntaxException {
        final SharedChannelPool.ChannelRequest channelRequest = new SharedChannelPool.ChannelRequest();
        final URI expectedChannelUri = null;
        assertEquals(expectedChannelUri, SharedChannelPool.getChannelRequestChannelURI(channelRequest));
    }

    @Test
    public void testGetChannelRequestChannelURIWithNoProxy() throws URISyntaxException {
        final SharedChannelPool.ChannelRequest channelRequest = new SharedChannelPool.ChannelRequest();
        channelRequest.destinationURI = new URI("http://other.example.com");
        final URI expectedChannelUri = new URI("http://other.example.com");
        assertEquals(expectedChannelUri, SharedChannelPool.getChannelRequestChannelURI(channelRequest));
    }

    @Test
    public void testGetChannelRequestChannelURIWithProxyWithPort80AndDestinationUriWithFtp() throws URISyntaxException {
        final SharedChannelPool.ChannelRequest channelRequest = new SharedChannelPool.ChannelRequest();
        channelRequest.destinationURI = new URI("ftp://other.example.com");
        channelRequest.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("my.example.com", 80));
        final URI expectedChannelUri = new URI("ftp://my.example.com:80");
        assertEquals(expectedChannelUri, SharedChannelPool.getChannelRequestChannelURI(channelRequest));
    }

    @Test
    public void testGetChannelRequestChannelURIWithProxyWithPort80AndDestinationUriWithHttp() throws URISyntaxException {
        final SharedChannelPool.ChannelRequest channelRequest = new SharedChannelPool.ChannelRequest();
        channelRequest.destinationURI = new URI("http://other.example.com");
        channelRequest.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("my.example.com", 80));
        final URI expectedChannelUri = new URI("http://my.example.com:80");
        assertEquals(expectedChannelUri, SharedChannelPool.getChannelRequestChannelURI(channelRequest));
    }

    @Test
    public void testGetChannelRequestChannelURIWithProxyWithPort80AndDestinationUriWithHttps() throws URISyntaxException {
        final SharedChannelPool.ChannelRequest channelRequest = new SharedChannelPool.ChannelRequest();
        channelRequest.destinationURI = new URI("https://other.example.com");
        channelRequest.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("my.example.com", 80));
        final URI expectedChannelUri = new URI("https://my.example.com:80");
        assertEquals(expectedChannelUri, SharedChannelPool.getChannelRequestChannelURI(channelRequest));
    }

    @Test
    public void testGetChannelRequestChannelURIWithProxyWithPort100AndDestinationUriWithFtp() throws URISyntaxException {
        final SharedChannelPool.ChannelRequest channelRequest = new SharedChannelPool.ChannelRequest();
        channelRequest.destinationURI = new URI("ftp://other.example.com");
        channelRequest.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("my.example.com", 100));
        final URI expectedChannelUri = new URI("ftp://my.example.com:100");
        assertEquals(expectedChannelUri, SharedChannelPool.getChannelRequestChannelURI(channelRequest));
    }

    @Test
    public void testGetChannelRequestChannelURIWithProxyWithPort100AndDestinationUriWithHttp() throws URISyntaxException {
        final SharedChannelPool.ChannelRequest channelRequest = new SharedChannelPool.ChannelRequest();
        channelRequest.destinationURI = new URI("http://other.example.com");
        channelRequest.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("my.example.com", 100));
        final URI expectedChannelUri = new URI("http://my.example.com:100");
        assertEquals(expectedChannelUri, SharedChannelPool.getChannelRequestChannelURI(channelRequest));
    }

    @Test
    public void testGetChannelRequestChannelURIWithProxyWithPort100AndDestinationUriWithHttps() throws URISyntaxException {
        final SharedChannelPool.ChannelRequest channelRequest = new SharedChannelPool.ChannelRequest();
        channelRequest.destinationURI = new URI("https://other.example.com");
        channelRequest.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("my.example.com", 100));
        final URI expectedChannelUri = new URI("https://my.example.com:100");
        assertEquals(expectedChannelUri, SharedChannelPool.getChannelRequestChannelURI(channelRequest));
    }

    @Test
    public void testGetChannelRequestChannelURIWithProxyWithPort443AndDestinationUriWithFtp() throws URISyntaxException {
        final SharedChannelPool.ChannelRequest channelRequest = new SharedChannelPool.ChannelRequest();
        channelRequest.destinationURI = new URI("ftp://other.example.com");
        channelRequest.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("my.example.com", 443));
        final URI expectedChannelUri = new URI("ftp://my.example.com:443");
        assertEquals(expectedChannelUri, SharedChannelPool.getChannelRequestChannelURI(channelRequest));
    }

    @Test
    public void testGetChannelRequestChannelURIWithProxyWithPort443AndDestinationUriWithHttp() throws URISyntaxException {
        final SharedChannelPool.ChannelRequest channelRequest = new SharedChannelPool.ChannelRequest();
        channelRequest.destinationURI = new URI("http://other.example.com");
        channelRequest.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("my.example.com", 443));
        final URI expectedChannelUri = new URI("http://my.example.com:443");
        assertEquals(expectedChannelUri, SharedChannelPool.getChannelRequestChannelURI(channelRequest));
    }

    @Test
    public void testGetChannelRequestChannelURIWithProxyWithPort443AndDestinationUriWithHttps() throws URISyntaxException {
        final SharedChannelPool.ChannelRequest channelRequest = new SharedChannelPool.ChannelRequest();
        channelRequest.destinationURI = new URI("https://other.example.com");
        channelRequest.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("my.example.com", 443));
        final URI expectedChannelUri = new URI("https://my.example.com:443");
        assertEquals(expectedChannelUri, SharedChannelPool.getChannelRequestChannelURI(channelRequest));
    }
}
