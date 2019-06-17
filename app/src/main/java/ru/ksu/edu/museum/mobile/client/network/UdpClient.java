package ru.ksu.edu.museum.mobile.client.network;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutionException;

public class UdpClient implements NetworkClient {
    private static final int MAX_DATA_SIZE = 65507;

    private SocketAddress serverSocketAddress;

    private UdpSendAsyncTask sendTask;
    private UdpReceiveAsyncTask receiveTask;

    @Override
    public void open() {
    }

    @Override
    public void close() {

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
            return receiveTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR).get().data;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void setServerSocketAddress(SocketAddress serverSocketAddress) {
        this.serverSocketAddress = serverSocketAddress;
    }

    private class UdpReceiveAsyncTask extends AsyncTask<Void, Void, DataHolder> {

        @Override
        protected DataHolder doInBackground(Void... voids) {
            DataHolder result = null;

            try (DatagramSocket receivingSocket = new DatagramSocket(serverSocketAddress)) {
                byte[] data = new byte[MAX_DATA_SIZE];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                receivingSocket.receive(packet);

                result = new DataHolder(packet.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result;
        }
    }

    private class UdpSendAsyncTask extends AsyncTask<DataHolder, Void, Void> {
        @Override
        protected Void doInBackground(DataHolder... dataHolders) {
            try (DatagramSocket sendingSocket = new DatagramSocket(serverSocketAddress)) {
                byte[] data = dataHolders[0].data;

                DatagramPacket packet = new DatagramPacket(data, data.length, serverSocketAddress);

                for (int i = 0; i < data.length / MAX_DATA_SIZE; i++) {
                    packet.setData(data, i * MAX_DATA_SIZE, MAX_DATA_SIZE);
                    sendingSocket.send(packet);
                }
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
