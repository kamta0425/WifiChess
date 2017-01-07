package com.kps.wificonnection;

import android.net.wifi.p2p.WifiP2pInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ChatActivity extends AppCompatActivity {

    public static final String TAG = "kamta";
    TextView tvMessage;
    WifiP2pInfo info;
    EditText editText;
    Button bSend;
    AsyncTask backgroundTask,writingTask;
    BlockingQueue<String> blockingQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        info=MainActivity.deviceInfo;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        tvMessage = (TextView) findViewById(R.id.message);
        editText = (EditText) findViewById(R.id.editText);
        bSend = (Button) findViewById(R.id.bSend);
        blockingQueue =new ArrayBlockingQueue<String>(20);

        bSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msgString=editText.getText().toString() ;
                if(msgString.length()==0)return;
                //writeToPeer(msgString);
                try {
                    blockingQueue.put(msgString);
                } catch (InterruptedException e) {
                    Log.d(TAG,"Blocking queue InterruptedException");
                }
                editText.setText("");
                tvMessage.append("Me   :- "+msgString+"\n");
                //InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                //inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });
        startConnection();
    }

    void startConnection(){
        Log.d(TAG,"startConnection");
        if(info==null){
            Log.d(TAG,"ChatActivity info is Null");
            Toast.makeText(this, "Device Info is Null", Toast.LENGTH_SHORT).show();
            return;
        }
        if ( info.groupFormed && info.isGroupOwner) {
            createServer();
        } else if (info.groupFormed) {
            joinServer();
        }
    }

    private void writeToSend(final DataOutputStream dout,final Socket socket) {
        writingTask = new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                while(socket.isConnected()) {
                    try {
                        final String msgSend = blockingQueue.take();
                        dout.writeUTF(msgSend);
                        dout.flush();
                    } catch (Exception e) {
                        Log.d(TAG, " Exception in writeToSend = " + e);
                    }
                }
                return null;
            }
        };
        writingTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,(Void[])null);
    }

    private void createServer() {
        backgroundTask = new AsyncTask() {
            @Override
            protected void onPreExecute() {
                Log.d(TAG, "onPreExecute called create server");
            }

            @Override
            protected Void doInBackground(Object[] params) {
                Log.d(TAG, "doInBackground Server");
                try {
                    ServerSocket serverSocket = new ServerSocket(8080);
                    serverSocket.setReuseAddress(true);
                    final Socket socket = serverSocket.accept();
                    Log.d(TAG, "Socket Accepted");
                    DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    writeToSend(dout,socket);
                    while(socket.isConnected()){
                        try {
                            final String msgReceived=dis.readUTF();      // blocked thread untill stream empty
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    tvMessage.append("Peer :- "+msgReceived+"\n");
                                }
                            });
                        } catch (IOException e) {
                            toast("IOException in writeToPeer writeObject");
                        }

                    }
                } catch (IOException e) {
                    Log.d(TAG, "IOException in create server" + e);
                } catch (Exception e) {
                    Log.d(TAG, "Exception in create server" + e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Log.d(TAG, "onPostExecute create server");
            }
        };
        backgroundTask.execute();
    }

    private void joinServer() {
        backgroundTask = new AsyncTask() {
            String hostip = info.groupOwnerAddress.getHostAddress();
            int port = 8080;
            @Override
            protected Void doInBackground(Object[] params) {
                Log.d(TAG, "doInBackground joinServer ");
                try {
                    //Thread.sleep(5000);
                    Socket socket = new Socket();
                    socket.setReuseAddress(true);
                    socket.bind(null);
                    socket.connect(new InetSocketAddress(hostip, port), 3000);
                    DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    writeToSend(dout,socket);
                    while(socket.isConnected()){
                        try {
                            final String msgReceived=dis.readUTF();      //blocked thread untill stream empty
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    tvMessage.append("Peer :- "+msgReceived+"\n");
                                }
                            });
                        } catch (IOException e) {
                            toast("IOException in writeToPeer writeObject");
                        }
                    }
                } catch (IOException e) {
                    Log.d(TAG, "IOException in joinServer" + e);
                } catch (Exception e) {
                    Log.d(TAG, "Exception in joinServer" + e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Log.d(TAG, "onPostExecute joinServer");
            }
        };
        backgroundTask.execute((Void[])null);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        if(backgroundTask!=null){
            backgroundTask.cancel(true);
            backgroundTask=null;
        }
        if(writingTask!=null){
            writingTask.cancel(true);
            writingTask=null;
        }
        super.onStop();
    }

    public  void toast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
