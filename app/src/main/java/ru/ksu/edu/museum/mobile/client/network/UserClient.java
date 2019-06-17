package ru.ksu.edu.museum.mobile.client.network;

import android.util.Log;

import org.opencv.core.Mat;

import java.io.Closeable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class UserClient implements Runnable, Closeable {
    private static final String TAG = "UserClient";

    private final String deviceId;
    private final String host;
    private final int port;
    private final int receivePort = 49853;

    private TcpClient tcpClient;
    private UdpClient udpClient;
    private JsonRpcService jsonRpcService;
    private InetAddress serverInetAddress;
    private SocketAddress serverSocketAddress;
    private String authToken;
    private String sceneId = "mock";

    public UserClient(String deviceId, String host, int port) {
        this.deviceId = deviceId;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        if (init()) {
            try {
                authToken = jsonRpcService.getAuthToken(deviceId);
            } catch (JsonRpcService.ResponseWithErrorException e) {
                e.printStackTrace();

                authToken = "admin";
            }

            try {
                serverSocketAddress = jsonRpcService
                        .initVideoStream(authToken, "mock", host, receivePort);
            } catch (JsonRpcService.ResponseWithErrorException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        try {
            tcpClient.close();
            udpClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFrame(Mat frame) {
        if (serverSocketAddress != null) {
            udpClient.setServerSocketAddress(serverSocketAddress);
            byte[] frameData =
                    new byte[(int) (frame.total() * frame.channels())];
            frame.get(0, 0, frameData);

            udpClient.send(frameData);
        }
    }

    private boolean init() {
        try {
            serverInetAddress = InetAddress.getByName(host);
            tcpClient = new TcpClient(serverInetAddress, port);
            udpClient = new UdpClient();
            jsonRpcService = new JsonRpcService(tcpClient, 1);

            return true;
        } catch (UnknownHostException e) {
            Log.e(TAG, e.getMessage());
        }

        return false;
    }
}
