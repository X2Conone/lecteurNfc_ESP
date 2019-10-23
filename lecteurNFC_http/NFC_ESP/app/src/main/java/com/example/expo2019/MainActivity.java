package com.example.expo2019;
/**
 * creat by klaus Monga 4B6555S for Expo Esis 2019 In G2 TLC
 * Work Team : arnold, arsen,nathan and other
 * this application read a nfc card and send the playload to a server
 *
 * */

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Parcelable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    public NfcAdapter nfcAdapter;
    public String matricule;
    private Socket socket;
    public int SERVERPORT = 5000;
    public String SERVER_IP = "0.0.0.0";
    public Button connect;
    public EditText serverIp, espip;
    String adresse_ip,adresse_esp;

    //Xavier modif
    AlertDialog alertDialog;
    AlertDialog.Builder builder;
    View dialogView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serverIp = (EditText)findViewById(R.id.serverip);
        espip  = (EditText)findViewById(R.id.espip);
        connect = (Button)findViewById(R.id.connect);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new ClientThread()).start();
            }
        });
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter!=null && nfcAdapter.isEnabled()){
            Toast.makeText(this, "ready...", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "not ready nfcadapter is null or Enabled", Toast.LENGTH_SHORT).show();
        }

        //Xavier: initialise la boite de Dialog
        builder=new AlertDialog.Builder(MainActivity.this);
    }

    @Override
    protected void onResume() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);
        IntentFilter[] intentFilter = new IntentFilter[]{};
        nfcAdapter.enableForegroundDispatch(this,pendingIntent,intentFilter,null);
        super.onResume();
    }

    @Override
    protected void onPause() {
        nfcAdapter.disableForegroundDispatch(this);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Toast.makeText(this, "new Intent card ...", Toast.LENGTH_SHORT).show();
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
        || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Toast.makeText(this, "New card Swapping", Toast.LENGTH_SHORT).show();
            Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            //byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] messages = new NdefMessage[1000];
            if (rawMsgs != null) {
                NdefMessage[] msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    messages[i] = (NdefMessage) rawMsgs[i];
                }
            }
            int i =0;

            if (rawMsgs!=null) {
                NdefRecord record = messages[i].getRecords()[i];
                byte[] idcard = record.getId();
                short tnf = record.getTnf();
                byte[] type = record.getType();
                matricule = getTextData(record.getPayload());
                Toast.makeText(this, "MAT: " + matricule, Toast.LENGTH_SHORT).show();
                //new Thread(new ClientThread()).start();
                if (serverIp.getText().toString().length()==0){
                    serverIp.setError("taper ip serveur");
                }else {
                    
                    sender("http://"+adresse_ip.toString()+"/snel/addingpresence.php?nom="+matricule,getApplicationContext());
                }

            }else {
                Toast.makeText(this, "this card is Empty", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this, "no swapping card", Toast.LENGTH_SHORT).show();
        }
        super.onNewIntent(intent);
    }
    public String getTextData(byte[] payload) {
        String texteCode = ((payload[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
        int langageCodeTaille = payload[0] & 0077;
        try {
            return new String(payload, langageCodeTaille + 1, payload.length - langageCodeTaille - 1, texteCode);
        } catch (UnsupportedEncodingException e) {
            Toast.makeText(this, "error "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return null;
    }
    class ClientThread implements Runnable {

        @Override
        public void run() {

            try {
                InetAddress serverAddr = InetAddress.getByName(adresse_ip.toString());

                socket = new Socket(serverAddr, Integer.valueOf(adresse_esp.toString()));
                try {
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())),
                            true);
                    out.println(matricule);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

    }

    public void sender(final String url, final Context context){
        final StringRequest stringRequest=new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("RESPONSE",response);
                Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
                if (response.equals("1")){
                    sendtoEsp("http://"+adresse_esp.toString()+"/ouvrir",getApplicationContext());
                }else {
                    Toast.makeText(context, "PLUS DE TICKET", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("errrrrr"," erreur de volley: ("+error.getMessage()+")");
                Toast.makeText(context, " erreur de volley: ("+error.getMessage()+")", Toast.LENGTH_SHORT).show();
                //sender(url,context);
            }
        });
        MySingleton.getInstance(context).addToRequestQueue(stringRequest);

    }


    //menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ip, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            builder.setTitle("Configurer une Adresse");
            builder.setIcon(R.drawable.l);
            LayoutInflater inflater = (MainActivity.this).getLayoutInflater();

            dialogView = inflater.inflate(R.layout.add_account, null);
            builder.setView(dialogView);

            //builder.setView(R.layout.add_account);

            builder.setPositiveButton("Annuler", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    alertDialog.dismiss();
                }
            });
            alertDialog = builder.create();
            alertDialog.show();

//            btn.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Toast.makeText(MesComptes.this, "On y click", Toast.LENGTH_SHORT).show();
//                }
//            });

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Xavier Modif: boutton boit de dialog
    public void Add_account(View v){
        EditText numerocompte=(EditText)dialogView.findViewById(R.id.serverip);
        EditText nomcompte=(EditText)dialogView.findViewById(R.id.espip);
        
        adresse_ip=numerocompte.getText().toString();
        adresse_esp=nomcompte.getText().toString();

        alertDialog.dismiss();

    }
    public void sendtoEsp(final String url, final Context context){
        final StringRequest stringRequest=new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("RESPONSE",response);
                Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("errrrrr"," erreur de volley: ("+error.getMessage()+")");
                Toast.makeText(context, " erreur de volley: ("+error.getMessage()+")", Toast.LENGTH_SHORT).show();
                //sender(url,context);
            }
        });
        MySingleton.getInstance(context).addToRequestQueue(stringRequest);

    }
}
