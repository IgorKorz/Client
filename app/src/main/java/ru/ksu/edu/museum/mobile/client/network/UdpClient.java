package ru.ksu.edu.museum.mobile.client.network;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutionException;

public class UdpClient implements NetworkClient {
    private static final int BUFFER_SIZE = 2500;
    private static final int MAX_DATA_SIZE = 65507;

    private InetAddress serverAddress;
    private InetAddress clientAddress;
    private int clientPort;
    private int serverPort;
    private UdpSendAsyncTask sendTask;
    private UdpReceiveAsyncTask receiveTask;

    public UdpClient(InetAddress clientAddress, int port) {
        this.clientAddress = clientAddress;
        this.clientPort = port;
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {
    }

    public void setServerAddress(InetAddress serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.serverPort = port;
    }

    @Override
    public void send(byte[] data) {
        sendTask = new UdpSendAsyncTask();
        sendTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new DataHolder(data));
    }

    @Override
    public byte[] receive() {
        receiveTask = new UdpReceiveAsyncTask();

        try {
            DataHolder dataHolder = receiveTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR).get();

            if (dataHolder != null) {
                return dataHolder.data;
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    private class UdpReceiveAsyncTask extends AsyncTask<Void, Void, DataHolder> {
        @Override
        protected DataHolder doInBackground(Void... voids) {
            try {
                DatagramSocket socket = new DatagramSocket(clientPort);
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.setBroadcast(true);
                socket.receive(packet);

                return new DataHolder(packet.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private class UdpSendAsyncTask extends AsyncTask<DataHolder, Void, Void> {
        @Override
        protected Void doInBackground(DataHolder... dataHolders) {
            try (DatagramSocket sendingSocket = new DatagramSocket()) {
                byte[] data = dataHolders[0].data;

                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                sendingSocket.setBroadcast(true);
                packet.setData(data, 0, Math.min(data.length, MAX_DATA_SIZE));
                sendingSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

    private class DataHolder {
        private byte[] data;

        private DataHolder(byte[] data) {
            this.data = data;
        }

        private DataHolder() {}
    }
}
