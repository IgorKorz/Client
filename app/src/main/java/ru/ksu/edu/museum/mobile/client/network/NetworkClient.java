package ru.ksu.edu.museum.mobile.client.network;

import java.io.Closeable;
import java.io.IOException;

public interface NetworkClient extends Closeable {
    void open() throws IOException;

    void close() throws IOException;

    void send(byte[] data) throws IOException;

    byte[] receive() throws IOException;
}
