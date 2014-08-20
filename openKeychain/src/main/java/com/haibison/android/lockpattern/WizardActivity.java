package com.haibison.android.lockpattern;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Arrays;


public class WizardActivity extends FragmentActivity implements SelectMethods.OnFragmentInteractionListener,
        Passphrase.OnFragmentInteractionListener, NFCFragment.OnFragmentInteractionListener{
    //create or authneticate
    public String selectedAction;
    //for lockpattern
    public static char[] pattern;
    private static String passphrase = "";
    //nfc string
    private static byte[] output = new byte[8];

    NfcAdapter adapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag myTag;
    boolean writeNFC = false;
    boolean readNFC = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActionBar() != null){
            getActionBar().setTitle(R.string.unlock_method);
        }

        selectedAction = getIntent().getExtras().getString("ACTION");
        if(savedInstanceState == null) {
            SelectMethods selectMethods = new SelectMethods();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.fragmentContainer, selectMethods).commit();
        }
        setContentView(R.layout.activity_wizard);

        adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter != null) {
            pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, WizardActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
            writeTagFilters = new IntentFilter[]{tagDetected};
        }
    }

    public void noPassphrase(View view){
        passphrase = "";
        Toast.makeText(this, R.string.no_passphrase_set, Toast.LENGTH_SHORT).show();
        this.finish();
    }

    public void passphrase(View view){
        Passphrase passphrase = new Passphrase();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, passphrase).addToBackStack(null).commit();
    }

    public void startLockpattern(View view) {
        if(getActionBar() != null) {
            getActionBar().setTitle(R.string.draw_lockpattern);
        }
        LockPatternFragment lpf = LockPatternFragment.newInstance(selectedAction);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, lpf).addToBackStack(null).commit();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
    public void cancel(View view) {
        this.finish();
    }

    public void savePassphrase(View view){
        EditText passphrase = (EditText) findViewById(R.id.passphrase);
        passphrase.setError(null);
        String pw = passphrase.getText().toString();
        //check and save passphrase
        if (selectedAction.equals(MainActivity.CREATE_METHOD)) {
            EditText passphraseAgain = (EditText) findViewById(R.id.passphraseAgain);
            passphraseAgain.setError(null);
            String pwAgain = passphraseAgain.getText().toString();

            if (!TextUtils.isEmpty(pw)) {
                if (!TextUtils.isEmpty(pwAgain)) {
                    if (pw.equals(pwAgain)) {
                        WizardActivity.passphrase = pw;
                        Toast.makeText(this, getString(R.string.passphrase_saved), Toast.LENGTH_SHORT).show();
                        this.finish();
                    } else {
                        passphrase.setError(getString(R.string.passphrase_invalid));
                        passphrase.requestFocus();
                    }
                } else {
                    passphraseAgain.setError(getString(R.string.missing_passphrase));
                    passphraseAgain.requestFocus();
                }
            } else {
                passphrase.setError(getString(R.string.missing_passphrase));
                passphrase.requestFocus();
            }
        }
        //check for right passphrase
        if (selectedAction.equals(MainActivity.AUTHENTICATION)){
            if(pw.equals(WizardActivity.passphrase)){
                Toast.makeText(this, getString(R.string.unlocked), Toast.LENGTH_SHORT).show();
                this.finish();
            } else {
                passphrase.setError(getString(R.string.passphrase_invalid));
                passphrase.requestFocus();
            }
        }
    }

    public void NFC(View view){
        if (adapter != null) {
            if (getActionBar() != null) {
                getActionBar().setTitle(R.string.nfc_title);
            }
            NFCFragment nfc = new NFCFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, nfc).addToBackStack(null).commit();

            //if you want to create a new method or just authenticate
            if (MainActivity.CREATE_METHOD.equals(selectedAction)) {
                writeNFC = true;
            } else if (MainActivity.AUTHENTICATION.equals(selectedAction)) {
                readNFC = true;
            }

            if (!adapter.isEnabled()) {
                showAlertDialog("Please enable NFC", true);
            }
        } else {
            showAlertDialog("This device is not supporting NFC", false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if (writeNFC && MainActivity.CREATE_METHOD.equals(selectedAction)) {
                //write new password on NFC tag
                try {
                    if (myTag != null) {
                        write(myTag);
                        writeNFC = false;   //just write once
                        Toast.makeText(this, "Successfully written to TAG!", Toast.LENGTH_SHORT).show();
                        //advance to lockpattern
                        LockPatternFragment lpf = LockPatternFragment.newInstance(selectedAction);
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.fragmentContainer, lpf).addToBackStack(null).commit();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (FormatException e) {
                    e.printStackTrace();
                }

            } else if (readNFC && MainActivity.AUTHENTICATION.equals(selectedAction)) {
                //read pw from NFC tag
                try {
                    if (myTag != null) {
                        //if tag detected, read tag
                        String pwtag = read(myTag);
                        if (output != null && pwtag.equals(output.toString())) {
                            //passwort matches, go to next view
                            Toast.makeText(this, "Matching password!", Toast.LENGTH_SHORT).show();
                            LockPatternFragment lpf = LockPatternFragment.newInstance(selectedAction);
                            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                            transaction.replace(R.id.fragmentContainer, lpf).addToBackStack(null).commit();
                            readNFC = false;    //just once
                        } else {
                            //passwort doesnt match
                            TextView nfc = (TextView) findViewById(R.id.nfcText);
                            nfc.setText(R.string.nfc_wrong_tag);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (FormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void write(Tag tag) throws IOException, FormatException {
        //generate new random key and write them on the tag
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(output);
        NdefRecord[] records = { createRecord(output.toString()) };
        NdefMessage message = new NdefMessage(records);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }


    private String read(Tag tag) throws IOException, FormatException {
        //read string from tag
        String password = null;
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        NdefMessage ndefMessage = ndef.getCachedNdefMessage();

        NdefRecord[] records = ndefMessage.getRecords();
        for (NdefRecord ndefRecord : records) {
            if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                try {
                    password =  readText(ndefRecord);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        ndef.close();
        return password;
    }

    private String readText(NdefRecord record) throws UnsupportedEncodingException {
        //low-level method for reading nfc
        byte[] payload = record.getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;
        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        //low-level method for writing nfc
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;
        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
    }

    public void showAlertDialog(String message, boolean nfc) {
        //This method shows an AlertDialog
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Information").setMessage(message).setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }
        );
        if (nfc) {
            //direct the user to the nfc settings to activate nfc
            alert.setNeutralButton("Settings",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                        }
                    }
            );
        }
        alert.show();
    }

    @Override
    public void onPause(){
        //pause this app and free nfc intent
        super.onPause();
        if (adapter != null) {
            WriteModeOff();
        }
    }

    @Override
    public void onResume(){
        //resume this app and get nfc intent
        super.onResume();
        if (adapter != null) {
            WriteModeOn();
        }
    }

    private void WriteModeOn(){
        //enable nfc for this view
        writeMode = true;
        adapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    private void WriteModeOff(){
        //disable nfc for this view
        writeMode = false;
        adapter.disableForegroundDispatch(this);
    }

}
