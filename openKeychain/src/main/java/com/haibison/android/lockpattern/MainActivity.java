package com.haibison.android.lockpattern;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    public static final String CREATE_METHOD = "create";
    public static final String AUTHENTICATION = "authenticate";

    NfcAdapter adapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    Tag mytag;
    boolean writeMode;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter = NfcAdapter.getDefaultAdapter(this);
        if(adapter==null){
            findViewById(R.id.selectLockpatternNFC).setVisibility(View.GONE);
        }

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };
    }

    /*@Override
    protected void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            mytag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Alert("Found Tag");
        }
    }*/


    public void createMethod(View view){
        Intent i = new Intent(this, WizardActivity.class);
        i.putExtra("ACTION", CREATE_METHOD );
        startActivity(i);
    }

    public void authenticate(View view) {
        Intent i = new Intent(this, WizardActivity.class);
        i.putExtra("ACTION", AUTHENTICATION );
        startActivity(i);
    }

    @Override
    public void onPause(){
        super.onPause();
        WriteModeOff();
    }

    @Override
    public void onResume(){
        super.onResume();
        WriteModeOn();
    }

    private void WriteModeOn(){
        writeMode = true;
        adapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    private void WriteModeOff(){
        writeMode = false;
        adapter.disableForegroundDispatch(this);
    }
}
