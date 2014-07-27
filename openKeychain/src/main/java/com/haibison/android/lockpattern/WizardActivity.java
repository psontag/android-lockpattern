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
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Arrays;


public class WizardActivity extends FragmentActivity implements SelectMethods.OnFragmentInteractionListener,
        Passphrase.OnFragmentInteractionListener, NFCFragment.OnFragmentInteractionListener{

    //contains information about the action to be performed - read or write?
    public String selectedAction;
    public static char[] pattern;
    private static String passphrase = "";

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
        Toast.makeText(this, "no passphrase set", Toast.LENGTH_SHORT).show();
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
                        this.passphrase = pw;
                        Toast.makeText(this, "passphrase saved", Toast.LENGTH_SHORT).show();
                        this.finish();
                    } else {
                        passphrase.setError("passphrase invalid");
                        passphrase.requestFocus();
                    }
                } else {
                    passphraseAgain.setError("missing passphrase");
                    passphraseAgain.requestFocus();
                }
            } else {
                passphrase.setError("missing passphrase");
                passphrase.requestFocus();
            }
        }
        //check for right passphrase
        if (selectedAction.equals(MainActivity.AUTHENTICATION)){
            if(pw.equals(this.passphrase)){
                Toast.makeText(this, "Unlocked", Toast.LENGTH_SHORT).show();
                this.finish();
            } else {
                passphrase.setError("passphrase invalid");
                passphrase.requestFocus();
            }
        }
    }

    public void NFC(View view){
        if (adapter != null) {
            if (getActionBar() != null) {
                getActionBar().setTitle(R.string.write_nfc);
            }
            NFCFragment nfc = new NFCFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, nfc).addToBackStack(null).commit();

            //In welchem Modus sind wir? Soll Intent lesen (=authentication) oder schreiben (=create)
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
                //Schreibe neues Passwort auf NFC Tag
                try {
                    if (myTag == null) {
                        //Alert("here is no Tag to write on.");
                        //Toast.makeText(context, "here is no Tag to write on.", Toast.LENGTH_SHORT).show();
                    } else {
                        write(myTag);
                        writeNFC = false;   //einmal schreiben reicht jetzt
                        Toast.makeText(this, "Successfully written to TAG!", Toast.LENGTH_SHORT).show();
                        //Gehe zum Lockpattern
                        FragmentTransaction Transaction = getSupportFragmentManager().beginTransaction();
                        Transaction.replace(R.id.fragmentContainer, LockPatternFragment.newInstance(selectedAction));
                        Transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                        Transaction.commit();
                    }
                } catch (IOException e) {
                    //Toast.makeText(this, "Error! Was the Tag close enough?", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    //Toast.makeText(this, "Error! Was the Tag close enough?", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else if (readNFC && MainActivity.AUTHENTICATION.equals(selectedAction)) {
                //Lese Passwort von NFC Tag
                try {
                    if (myTag == null) {
                        //return "There is no Tag to read from.";
                    } else {
                        String pwtag = null;
                        pwtag = read(myTag);
                        if (pwtag.equals("passwort")) {
                            Toast.makeText(this, "Matching password!", Toast.LENGTH_SHORT).show();
                            readNFC = false;
                        } else {
                            Toast.makeText(this, "The passwords do not match!", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (IOException e) {
                    showAlertDialog("Error! Was the Tag close enough?", false);
                    e.printStackTrace();
                } catch (FormatException e) {
                    showAlertDialog("Error! Was the Tag close enough?", false);
                    e.printStackTrace();
                }
            }
        }
    }

    private void write(Tag tag) throws IOException, FormatException {
        SecureRandom sr = new SecureRandom();
        byte[] output = new byte[8];
        sr.nextBytes(output);

        NdefRecord[] records = { createRecord(output.toString()) };
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        // Enable I/O
        ndef.connect();
        // Write the message
        ndef.writeNdefMessage(message);
        // Close the connection
        ndef.close();
    }

    private String read(Tag tag) throws IOException, FormatException {
        String password = null;
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        // Enable I/O
        ndef.connect();
        // Read the message
        NdefMessage ndefMessage = ndef.getCachedNdefMessage();

        NdefRecord[] records = ndefMessage.getRecords();
        for (NdefRecord ndefRecord : records) {
            if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                try {
                    password =  readText(ndefRecord);
                } catch (UnsupportedEncodingException e) {
                }
            }
        }
        // Close the connection
        ndef.close();
        return password;
    }

    private String readText(NdefRecord record) throws UnsupportedEncodingException {
        byte[] payload = record.getPayload();
        // Get the Text Encoding
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        // Get the Language Code
        int languageCodeLength = payload[0] & 0063;
        // Get the Text
        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
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

    public void showAlertDialog(String message, Boolean nfc) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Information").setMessage(message).setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }
        );
        if (nfc) {
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
        super.onPause();
        if (adapter != null) {
            WriteModeOff();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if (adapter != null) {
            WriteModeOn();
        }
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
