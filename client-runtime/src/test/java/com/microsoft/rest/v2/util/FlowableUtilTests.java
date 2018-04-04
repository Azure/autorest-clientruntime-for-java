package com.microsoft.rest.v2.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import com.google.common.io.Files;

import io.reactivex.schedulers.Schedulers;

public class FlowableUtilTests {

    @Test
    public void testCanReadSlice() throws IOException {
        File file = new File("target/test1");
        Files.write("hello there".getBytes(StandardCharsets.UTF_8), file);
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ);
        byte[] bytes = FlowableUtil.readFile(channel, 1, 3) //
                .map(bb -> toBytes(bb)) //
                .collectInto(new ByteArrayOutputStream(), (bos, b) -> bos.write(b)) //
                .blockingGet().toByteArray();
        assertEquals("ell", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testCanReadEmptyFile() throws IOException {
        File file = new File("target/test2");
        file.delete();
        file.createNewFile();
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ);
        byte[] bytes = FlowableUtil.readFile(channel, 1, 3) //
                .map(bb -> toBytes(bb)) //
                .collectInto(new ByteArrayOutputStream(), (bos, b) -> bos.write(b)) //
                .blockingGet().toByteArray();
        assertEquals(0, bytes.length);
    }

    @Test
    public void testAsynchronyShortInput() throws IOException {
        File file = new File("target/test3");
        Files.write("hello there".getBytes(StandardCharsets.UTF_8), file);
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ);
        byte[] bytes = FlowableUtil.readFile(channel) //
                .map(bb -> toBytes(bb)) //
                .rebatchRequests(1) //
                .subscribeOn(Schedulers.io()) //
                .observeOn(Schedulers.io()) //
                .collectInto(new ByteArrayOutputStream(), (bos, b) -> bos.write(b)) //
                .blockingGet() //
                .toByteArray();
        assertEquals("hello there", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testAsynchronyLongInput() throws IOException, NoSuchAlgorithmException {
        File file = new File("target/test4");
        byte[] array = "1234567690".getBytes(StandardCharsets.UTF_8);
        int n = 1_000_000;
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            for (int i = 0; i < 1_000_000; i++) {
                out.write(array);
                digest.update(array);
            }
        }
        byte[] expected = digest.digest();
        digest.reset();
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ);
        FlowableUtil.readFile(channel) //
                .rebatchRequests(1) //
                .subscribeOn(Schedulers.io()) //
                .observeOn(Schedulers.io()) //
                .blockingForEach(bb -> digest.update(bb));
                
        assertArrayEquals(expected, digest.digest());
    }

    private static byte[] toBytes(ByteBuffer bb) {
        byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);
        return bytes;
    }

}
