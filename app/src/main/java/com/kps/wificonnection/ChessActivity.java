package com.kps.wificonnection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ChessActivity extends Activity {

    public static final String TAG = "kamta";
    GridView mygrid;
    GridAdapter gridadapter;
    static boolean isCellActive = false,isMyTurn=false;
    static int prevCellIndex = 0;
    static View prevView = null;
    static ImageView currImageView, prevImageView;
    static int prevColor = 0;
    int[] imageList;
    int pieceChoice = 1;
    static int blackrook, blackknight, blackbishop, blackpawn, blackqueen, blackking;
    static int whiteking, whitequeen, whitepawn, whiterook, whiteknight, whitebishop;
    Button bConnect;
    TextView tvMessage;
    Intent intent;
    WifiP2pInfo info;
    AsyncTask<Void,Void,Void> backgroundTask;
    BlockingQueue<String> blockingQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chess);

        intent = this.getIntent();
        info=MainActivity.deviceInfo;
        blockingQueue =new ArrayBlockingQueue<String>(1);

        //int pieceChoice = intent.getIntExtra("pieceChoice",-1); // pieceChoice=1 for white & 2 for black of own piece
        bConnect = (Button) findViewById(R.id.bConnect);
        tvMessage = (TextView) findViewById(R.id.tvMessage);
        startConnection();
        imageList = new int[64];
        setPieces();
        mygrid = (GridView) findViewById(R.id.gridView1);
        mygrid.setAdapter(new GridAdapter(this, pieceChoice));

        mygrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View currView, int currCellIndex, long arg3) {

                // check color of piece here first if others piece is clicked then return directly

                if(!isMyTurn){
                    toast("Wait its Peer`s Turn ");
                    return;
                }
                currImageView = (ImageView) currView.findViewById(R.id.imageView1);
                boolean isVacant = imageList[currCellIndex] == 0;
                if (prevCellIndex == currCellIndex || (isVacant && !isCellActive)) {
                    if (isCellActive) {
                        if (prevColor == Color.RED)
                            ((FrameLayout) currView.findViewById(R.id.frame)).setBackgroundColor(Color.RED);
                        else
                            ((FrameLayout) currView.findViewById(R.id.frame)).setBackgroundColor(Color.WHITE);
                        prevCellIndex = -1;
                    }
                    isCellActive = false;
                    return;
                }

                if (isCellActive) {                   // player already pickupd for move and isCellActive set
                    prevView = mygrid.getChildAt(prevCellIndex);
                    prevImageView = (ImageView) prevView.findViewById(R.id.imageView1);
                    if (!isVacant && isSamePiece(prevCellIndex, currCellIndex)) return;
                    try {
                        blockingQueue.put(prevCellIndex+" "+currCellIndex);
                        isMyTurn=false;
                    } catch (Exception e) {
                        Log.d(TAG,"Exception while adding to BlockingQueue"+e);
                        toast("InterruptedException while adding to BlockingQueue"+e);
                    }
                    currImageView.setImageDrawable(prevImageView.getDrawable());
                    prevImageView.setImageDrawable(null);
                    imageList[currCellIndex] = imageList[prevCellIndex];
                    imageList[prevCellIndex] = 0;
                    if (prevColor == Color.RED)
                        ((FrameLayout) prevView.findViewById(R.id.frame)).setBackgroundColor(Color.RED);
                    else
                        ((FrameLayout) prevView.findViewById(R.id.frame)).setBackgroundColor(Color.WHITE);
                    isCellActive = false;
                    prevCellIndex = -1;
                } else {                          // player pickupd for move
                    prevCellIndex = currCellIndex;
                    isCellActive = true;
                    FrameLayout frameLayout = (FrameLayout) currView.findViewById(R.id.frame);
                    prevColor = checkBlack(currCellIndex) ? Color.RED : Color.WHITE;
                    frameLayout.setBackgroundColor(Color.parseColor("#13F9EF"));
                }
            }
        });

        bConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(info==null){
                    toast("info is null");
                    return;
                }
                if(info.groupFormed && !info.isGroupOwner){
                    joinServer();
                    tvMessage.setText("Connecting to server....");
                    tvMessage.setTextColor(Color.GREEN);
                }
            }
        });
    }

    void startConnection(){
        if(info==null){
            Log.d(TAG,"info is Null");
            toast("Device Info is Null");
            return;
        }
        if ( info.groupFormed && info.isGroupOwner) {
            createServer();
            isMyTurn=true;
            bConnect.setVisibility(View.GONE);
            tvMessage.setText("Waiting for client to connect");
            tvMessage.setTextColor(Color.GREEN);
        } else if (info.groupFormed) {
            //joinServer();
            pieceChoice=2;
        }
    }

    private boolean isSamePiece(int prevCellIndex, int currCellIndex) {
        if (whitebishop <= imageList[currCellIndex] && imageList[currCellIndex] <= whiterook) {
            if (whitebishop <= imageList[prevCellIndex] && imageList[prevCellIndex] <= whiterook)
                return true;
            else return false;
        } else {
            if (whitebishop <= imageList[prevCellIndex] && imageList[prevCellIndex] <= whiterook)
                return false;
            else return true;
        }
    }

    private void setPieces() {
        blackrook = R.drawable.blackrook;
        blackking = R.drawable.blackking;
        blackknight = R.drawable.blackknight;
        blackbishop = R.drawable.blackbishop;
        blackpawn = R.drawable.blackpawns;
        blackqueen = R.drawable.blackqueen;

        whiteking = R.drawable.whiteking;
        whitequeen = R.drawable.whitequeen;
        whitepawn = R.drawable.whitepawns;
        whiterook = R.drawable.whiterook;
        whiteknight = R.drawable.whiteknight;
        whitebishop = R.drawable.whitebishop;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private static boolean checkBlack(int x) {
        if ((x / 8) % 2 == 0) {
            if (x % 2 == 0) return true;
            else return false;
        } else {
            if (x % 2 == 0) return false;
            else return true;
        }
    }

    class GridAdapter extends BaseAdapter {     // Inner Class

        class ViewHolder {
            ImageView goti;

            ViewHolder(View v) {
                goti = (ImageView) v.findViewById(R.id.imageView1);
            }
        }

        Context context;
        int choice;

        GridAdapter(Context context, int choice) {
            this.context = context;
            this.choice = choice;
            for (int i = 0; i <= 63; i++) {
                if (i >= 16 && i <= 47) imageList[i] = 0;
                else if (choice == 1) {
                    if (i >= 0 && i <= 7) {
                        if (i == 0 || i == 7) imageList[i] = blackrook;
                        else if (i == 1 || i == 6) imageList[i] = blackknight;
                        else if (i == 2 || i == 5) imageList[i] = blackbishop;
                        else if (i == 3) imageList[i] = blackking;
                        else if (i == 4) imageList[i] = blackqueen;
                    } else if (i >= 8 && i <= 15) {
                        imageList[i] = blackpawn;
                    } else if (i >= 48 && i <= 55) {
                        imageList[i] = whitepawn;
                    } else if (i >= 56 && i <= 63) {
                        if (i == 56 || i == 63) imageList[i] = whiterook;
                        else if (i == 57 || i == 62) imageList[i] = whiteknight;
                        else if (i == 58 || i == 61) imageList[i] = whitebishop;
                        else if (i == 59) imageList[i] = whiteking;
                        else if (i == 60) imageList[i] = whitequeen;
                    }
                } else { // pieceChoice ==2
                    if (i >= 0 && i <= 7) {
                        if (i == 0 || i == 7) imageList[i] = whiterook;
                        else if (i == 1 || i == 6) imageList[i] = whiteknight;
                        else if (i == 2 || i == 5) imageList[i] = whitebishop;
                        else if (i == 3) imageList[i] = whiteking;
                        else if (i == 4) imageList[i] = whitequeen;
                    } else if (i >= 8 && i <= 15) {
                        imageList[i] = whitepawn;
                    } else if (i >= 48 && i <= 55) {
                        imageList[i] = blackpawn;
                    } else if (i >= 56 && i <= 63) {
                        if (i == 56 || i == 63) imageList[i] = blackrook;
                        else if (i == 57 || i == 62) imageList[i] = blackknight;
                        else if (i == 58 || i == 61) imageList[i] = blackbishop;
                        else if (i == 59) imageList[i] = blackking;
                        else if (i == 60) imageList[i] = blackqueen;
                    }
                }
            }
        }

        @Override
        public int getCount() {
            return 64;
        }

        @Override
        public Object getItem(int arg0) {
            return imageList[arg0];
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(int arg0, View view, ViewGroup arg2) {
            View row = view;
            ViewHolder holder = null;

            if (row == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.single_item, arg2, false);
                holder = new ViewHolder(row);
                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }
            FrameLayout frame = (FrameLayout) row.findViewById(R.id.frame);
            if (checkBlack(arg0)) {
                frame.setBackgroundColor(Color.RED);
            } else {
                frame.setBackgroundColor(Color.WHITE);
            }
            if (imageList[arg0] != 0) holder.goti.setImageResource(imageList[arg0]);
            return row;
        }
    }

    public void movePeersPiece(final String moveStr){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String s[]= moveStr.split(" ");
                int start=63-Integer.parseInt(s[0]);
                int end=63-Integer.parseInt(s[1]);
                View prevView = mygrid.getChildAt(start);
                ImageView prevImageView = (ImageView) prevView.findViewById(R.id.imageView1);
                ((ImageView) mygrid.getChildAt(end).findViewById(R.id.imageView1) ).setImageDrawable(prevImageView.getDrawable());
                prevImageView.setImageDrawable(null);
            }
        });
    }

    private void createServer() {
        backgroundTask = new AsyncTask<Void,Void,Void>() {
            @Override
            protected void onPreExecute() {
                Log.d(TAG, "onPreExecute called create server");
            }

            @Override
            protected Void doInBackground(Void... v) {
                Log.d(TAG, "doInBackground Server");
                try {
                    ServerSocket serverSocket = new ServerSocket(8080);
                    serverSocket.setReuseAddress(true);
                    Socket clientSocket = serverSocket.accept();
                    Log.d(TAG, "Socket Accepted");
                    ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                    ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
                    if(clientSocket.isConnected() ){
                        setStatusFirstMove("Client Connected Yours Turn...",Color.GREEN);
                    }
                    Log.d(TAG, "Loop Stated ");
                    while(clientSocket.isConnected()){
                        try {
                            String moveStr = blockingQueue.take();
                            oos.writeObject(moveStr);
                            oos.flush();
                            setStatus("Peer`s Turn...",Color.RED);

                            moveStr = (String) ois.readObject();      // blocked thread untill stream empty
                            Log.d(TAG, "Piece readed = "+moveStr);
                            movePeersPiece(moveStr);
                            isMyTurn=true;
                            setStatus("Yours`s Turn...",Color.GREEN);

                        } catch (Exception e) {
                            toast("Exception in writeToPeer writeObject = "+e);
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
            protected void onPostExecute(Void v) {
                Log.d(TAG, "onPostExecute create server");
            }
        };
        backgroundTask.execute();
    }

    private void joinServer() {
        backgroundTask = new AsyncTask<Void,Void,Void>() {
            String hostip = info.groupOwnerAddress.getHostAddress();
            int port = 8080;
            Socket clientSocket=null;

            @Override
            protected void onPreExecute() {
                Log.d(TAG, "onPreExecute called joinServer HostIP : "+hostip);
            }

            @Override
            protected Void doInBackground(Void... v) {
                Log.d(TAG, "doInBackground joinServer ");
                try {
                    clientSocket = new Socket();
                    clientSocket.setReuseAddress(true);
                    clientSocket.bind(null);
                    clientSocket.connect(new InetSocketAddress(hostip, port), 3000);
                    ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                    ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
                    if(clientSocket.isConnected() ){
                        setStatusFirstMove("Connected to Peer, Peer`s Turn...",Color.RED);
                    }
                    Log.d(TAG, "Loop Stated ");
                    while(clientSocket.isConnected()){
                        try {
                            String moveStr= (String) ois.readObject();      // blocked thread untill stream empty
                            movePeersPiece(moveStr);
                            isMyTurn=true;
                            setStatus("Your`s Turn...",Color.GREEN);
                            Log.d(TAG, "Piece readed = "+moveStr);

                            moveStr = blockingQueue.take();
                            oos.writeObject(moveStr);
                            oos.flush();
                            setStatus("Peer`s Turn...",Color.RED);

                        } catch (Exception e) {
                            toast("Exception in writeToPeer writeObject = "+e);
                        }
                    }
                    if(oos!=null)oos.close();
                    if(ois!=null)ois.close();
                    if(clientSocket!=null){clientSocket.close();clientSocket=null;}
                } catch (IOException e) {
                    Log.d(TAG, "IOException in joinServer" + e);
                    if(clientSocket!=null){
                        try {clientSocket.close();}
                        catch (IOException e1) {}
                        clientSocket=null;}
                } catch (Exception e) {
                    Log.d(TAG, "Exception in joinServer" + e);
                }
                return null;
            }


            @Override
            protected void onPostExecute(Void v) {
                Log.d(TAG, "onPostExecute joinServer");
            }
        };
        backgroundTask.execute((Void[])null);
    }

    private void setStatus(final String message,final  int color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvMessage.setText(message);
                tvMessage.setTextColor(color);
            }
        });
    }

    private void setStatusFirstMove(final String message, final int color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvMessage.setText(message);
                tvMessage.setTextColor(color);
                if(color==Color.RED){                   // client is moved (black piece)
                    bConnect.setVisibility(View.GONE);
                }else{                                   //Server is moved (white color)

                }
            }
        });
    }

    public  void toast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy() ChessActivity" );
        if(backgroundTask!=null){
            backgroundTask.cancel(true);
            backgroundTask=null;
        }
    }
}