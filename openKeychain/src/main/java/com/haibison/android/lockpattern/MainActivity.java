package com.haibison.android.lockpattern;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    public static final String CREATE_METHOD = "create";
    public static final String AUTHENTICATION = "authenticate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void createMethod(View view){
        Intent i = new Intent(this, WizardActivity.class);
        i.putExtra("ACTION", CREATE_METHOD );
        startActivity(i);
    }

    public void authenticate(View view) {
        Intent i = new Intent(this, WizardActivity.class);
        i.putExtra("ACTION", AUTHENTICATION);
        startActivity(i);
    }

}
