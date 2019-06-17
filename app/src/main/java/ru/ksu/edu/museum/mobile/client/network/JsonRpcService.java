package ru.ksu.edu.museum.mobile.client.network;

import android.os.AsyncTask;
import android.util.Pair;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import net.minidev.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class JsonRpcService {
    //region methods names
    private static final String ACK = "AckAsync";
    private static final String INIT_VIDEO_STREAM = "InitVideoStreamAsync";
    private static final String CLOSE_VIDEO_STREAM = "CloseVideoStreamAsync";
    private static final String GET_AUTH_TOKEN = "GetAuthTokenAsync";
    private static final String GET_SCENE_DATA = "GetSceneDataAsync";
    //endregion
    //region headers
    private static final String CONTENT_LENGTH = "Content-Length";
    //endregion
    //region params names
    private static final String MACHINE_ID = "machineId";
    private static final String AUTH_TOKEN = "authToken";
    private static final String SCENE_ID = "sceneId";
    private static final String DATA_RECEIVE_IP = "dataRecieveIP";
    private static final String DATA_RECEIVE_PORT = "dataRecievePort";
    //endregion

    private final int id;
    private final TcpClient tcpClient;

    public JsonRpcService(TcpClient tcpClient, int id) {
        this.tcpClient = tcpClient;
        this.id = id;
    }

    public boolean ack(String authToken) {
        Map<String, Object> params = new HashMap<>();
        params.put(AUTH_TOKEN, authToken);

        JSONRPC2Response response = initRequest(ACK, params);

        return response.getError() != null;
    }

    public InetSocketAddress initVideoStream(String authToken,
                                             String sceneId,
                                             String dataReceiveIp,
                                             int dataReceivePort) {

        Map<String, Object> params = new HashMap<>();
        params.put(AUTH_TOKEN, authToken);
        params.put(SCENE_ID, sceneId);
        params.put(DATA_RECEIVE_IP, dataReceiveIp);
        params.put(DATA_RECEIVE_PORT, dataReceivePort);

        JSONRPC2Response response = initRequest(INIT_VIDEO_STREAM, params);
        InetSocketAddress result = null;

        if (response != null) {
            if (response.getError() == null) {
                Map<String, Object> resultMap = responseToMap(response);
                String hostname = (String) resultMap.get("Item1");
                Long port = (Long) resultMap.get("Item2");

                if (hostname != null && port != null) {
                    result = new InetSocketAddress(hostname, port.intValue());
                }
            } else {
                throw new ResponseWithErrorException(response.getError().getMessage());
            }
        }

        return result;
    }

    public void closeVideoStream(String authToken, String videoStreamToken) {
    }

    public String getAuthToken(String machineId) {
        Map<String, Object> params = new HashMap<>();
        params.put(MACHINE_ID, machineId);

        JSONRPC2Response response = initRequest(GET_AUTH_TOKEN, params);
        String result = null;

        if (response != null) {
            if (response.getError() == null) {
                result = response.getResult().toString();
            } else {
                throw new ResponseWithErrorException(response.getError().getMessage());
            }
        }

        return result;
    }

    public Long getSceneData(String authToken, String sceneId) {
        return null;
    }

    private JSONRPC2Response initRequest(String methodName, Map<String, Object> params) {
        JSONRPC2Response result = null;
        JSONRPC2Request request = new JSONRPC2Request(methodName, params, id);
        JsonRpcTask task  = new JsonRpcTask();
        task.execute(new Pair<>(tcpClient, request));

        try {
            result = task.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        return result;
    }

    private Map<String, Object> responseToMap(JSONRPC2Response response) {
        return (HashMap<String, Object>) response.getResult();
    }

    public static class JsonRpcTask
            extends AsyncTask<Pair<TcpClient, JSONRPC2Request>, Void, JSONRPC2Response> {

        @SafeVarargs
        @Override
        protected final JSONRPC2Response doInBackground(Pair<TcpClient, JSONRPC2Request>... pairs) {
            for (Pair<TcpClient, JSONRPC2Request> pair : pairs)
                try {
                    TcpClient tcpClient = pair.first;
                    tcpClient.open();
                    PrintWriter writer = new PrintWriter(tcpClient.getOutputStream());
                    BufferedReader reader =
                            new BufferedReader(new InputStreamReader(tcpClient.getInputStream()));
                    String content = pair.second.toJSONString();

                    writer.write(String.format(Locale.ENGLISH, "%s: %d\r\n\r\n",
                            CONTENT_LENGTH, content.length()));
                    writer.write(content);
                    writer.flush();

                    String response = reader.readLine();
                    reader.readLine();

                    int length = Integer.parseInt(response
                            .substring(response.lastIndexOf(':') + 2));
                    char[] result = new char[length];
                    reader.read(result, 0, length);
                    String json = new String(result);

                    tcpClient.close();
                    writer.close();
                    reader.close();

                    return JSONRPC2Response.parse(json);
                } catch (IOException | JSONRPC2ParseException e) {
                    e.printStackTrace();
                }

            return null;
        }
    }

    public class ResponseWithErrorException extends RuntimeException {
        private ResponseWithErrorException(String message) {
            super(message);
        }
    }
}
