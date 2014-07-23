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
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;


public class WizardActivity extends FragmentActivity implements SelectMethods.OnFragmentInteractionListener,
        Passphrase.OnFragmentInteractionListener, NFCFragment.OnFragmentInteractionListener{

    //contains information about the action to be performed - read or write?
    public String selectedAction;
    public static char[] pattern;

    //Kann man hier auf die MainActivity drauf zugreifen? redundant
    public static final String CREATE_METHOD = "create";
    public static final String AUTHENTICATION = "authenticate";

    NfcAdapter adapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag mytag;
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
        pendingIntent = PendingIntent.getActivity(this , 0, new Intent(this, WizardActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };
        //adapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);




    }
public void noPassphrase(View view){
    //TODO set no passphrase
    Toast.makeText(this, "No passphrase set", Toast.LENGTH_SHORT).show();
    finish();
}


    public void passphrase(View view){
        if(getActionBar() != null) {
            getActionBar().setTitle(R.string.set_passphrase);
        }
        Passphrase passphrase = new Passphrase();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, passphrase).addToBackStack(null).commit();

    }



    public void startLockpattern(View view) {
        if(getActionBar() != null) {
            getActionBar().setTitle(R.string.draw_lockpattern);
        }

        //Creating a new Instance of LockPatternFragment with information about selected method.
        LockPatternFragment lpf = LockPatternFragment.newInstance(selectedAction);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, lpf).addToBackStack(null).commit();
        //Context context = getApplicationContext();
    }



    @Override
    public void onFragmentInteraction(Uri uri) {

    }
    public void cancel(View view) {
        this.finish();
    }

    public void savePassphrase(View view){
        EditText passphrase = (EditText) findViewById(R.id.passphrase);
        EditText passphraseAgain = (EditText) findViewById(R.id.passphraseAgain);
        passphrase.setError(null);
        passphraseAgain.setError(null);

        String pw = passphrase.getText().toString();
        String pwAgain = passphraseAgain.getText().toString();

        if(!TextUtils.isEmpty(pw)) {
            if (!TextUtils.isEmpty(pwAgain)) {

                if (pw.equals(pwAgain)) {
                    // TODO save the pw somewhere
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

    public void writeNFC(View view){
        NFCFragment nfc = new NFCFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, nfc).addToBackStack(null).commit();
        //Zeig,dass du hier bist

        //In welchem Modus sind wir? Soll Intent lesen (=authentication) oder schreiben (=create)
        if (CREATE_METHOD.equals(selectedAction)) {
            writeNFC = true;
        } else if (AUTHENTICATION.equals(selectedAction)) {
            readNFC = true;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            mytag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if (writeNFC && CREATE_METHOD.equals(selectedAction)) {
                //Schreibe neues Passwort auf NFC Tag
                try {
                    if (mytag == null) {
                        //Alert("here is no Tag to write on.");
                        //Toast.makeText(context, "here is no Tag to write on.", Toast.LENGTH_SHORT).show();
                    } else {
                        write("passwort", mytag);
                        writeNFC = false;   //einmal schreiben reicht jetzt
                        Toast.makeText(this, "Successfully written to TAG!", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    //Toast.makeText(this, "Error! Was the Tag close enough?", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    //Toast.makeText(this, "Error! Was the Tag close enough?", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else if (readNFC && AUTHENTICATION.equals(selectedAction)) {
                //Lese Passwort von NFC Tag
                try {
                    if(mytag==null){
                        //return "There is no Tag to read from.";
                    } else {
                        String pwtag = null;
                        pwtag = read(mytag);
                        if (pwtag.equals("passwort")) {
                            Toast.makeText(this, "Matching password!", Toast.LENGTH_SHORT).show();
                            readNFC = false;
                        } else {
                            Toast.makeText(this, "The passwords do not match!", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (IOException e) {
                    Alert("Error! Was the Tag close enough?");
                    e.printStackTrace();
                } catch (FormatException e) {
                    Alert("Error! Was the Tag close enough?");
                    e.printStackTrace();
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error! Was the Tag close enough?", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } catch (FormatException e) {
                Toast.makeText(this, "Error! Was the Tag close enough?", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void write(String text, Tag tag) throws IOException, FormatException {

        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        // Enable I/O
        ndef.connect();
        // Write the message
        ndef.writeNdefMessage(message);
        // Close the connection
        ndef.close();
        Toast.makeText(this, "Wrote NFC!", Toast.LENGTH_SHORT).show();
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
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

        byte[] payload = record.getPayload();

        // Get the Text Encoding
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

        // Get the Language Code
        int languageCodeLength = payload[0] & 0063;

        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
        // e.g. "en"

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

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);

        return recordNFC;
    }

    public void Alert(String message){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Information");
        alert.setMessage(message);
        alert.setPositiveButton("Okay",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
        alert.show();
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
