package ru.ksu.edu.museum.mobile.client.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class TcpClient implements NetworkClient {
    private static final int BUFFER_SIZE = 512;

    private final InetAddress serverInetAddress;
    private final int port;

    private Socket clientSocket;
    private InputStream inputStream;
    private OutputStream outputStream;

    TcpClient(InetAddress serverInetAddress, int port) {
        this.serverInetAddress = serverInetAddress;
        this.port = port;
    }

    @Override
    public void open() throws IOException {
        clientSocket = new Socket(serverInetAddress, port);
        inputStream = clientSocket.getInputStream();
        outputStream = clientSocket.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        outputStream.close();
        clientSocket.close();
    }

    @Override
    public void send(byte[] data) throws IOException {
        outputStream.write(data);
        outputStream.flush();
    }

    @Override
    public byte[] receive() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesCount = inputStream.read(buffer);
        byte[] result = new byte[bytesCount];
        System.arraycopy(buffer, 0, result, 0, bytesCount);

        return result;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}
