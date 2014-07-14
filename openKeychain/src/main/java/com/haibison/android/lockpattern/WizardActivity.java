package com.haibison.android.lockpattern;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class WizardActivity extends FragmentActivity implements SelectMethods.OnFragmentInteractionListener,
        Passphrase.OnFragmentInteractionListener, NFC.OnFragmentInteractionListener{

    //contains information about the action to be performed - read or write?
    public String selectedAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActionBar() != null){
            getActionBar().setTitle(R.string.unlock_method);
        }

        selectedAction = getIntent().getExtras().getString("ACTION");
        SelectMethods selectMethods = new SelectMethods();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragmentContainer, selectMethods).addToBackStack(null).commit();
        setContentView(R.layout.activity_wizard);
    }


    public void passphrase(View view){
        if(getActionBar() != null) {
            getActionBar().setTitle(R.string.set_passphrase);
        }
        Passphrase passphrase = new Passphrase();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, passphrase).addToBackStack(null).commit();

    }

    public void writeNFC(View view){

        NFC nfc = new NFC();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, nfc).addToBackStack(null).commit();
        //Zeig,dass du hier bist
        Context context = getApplicationContext();
        Toast.makeText(context, "Got here", Toast.LENGTH_SHORT).show();
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
}
