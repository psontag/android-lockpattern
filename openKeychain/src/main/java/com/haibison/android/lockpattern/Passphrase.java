package com.haibison.android.lockpattern;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.haibison.android.lockpattern.Passphrase.OnFragmentInteractionListener} interface
 * to handle interaction events.
 *
 */
public class Passphrase extends Fragment {

    private OnFragmentInteractionListener mListener;

    public Passphrase() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_passphrase, container, false);
        EditText passphraseAgain = (EditText) view.findViewById(R.id.passphraseAgain);
        TextView passphraseText = (TextView) view.findViewById(R.id.passphraseText);
        TextView passphraseTextAgain = (TextView) view.findViewById(R.id.passphraseTextAgain);
        String selectedAction = getActivity().getIntent().getExtras().getString("ACTION");
        if(selectedAction.equals(MainActivity.AUTHENTICATION)) {
            passphraseAgain.setVisibility(View.GONE);
            passphraseTextAgain.setVisibility(View.GONE);
            passphraseText.setText(R.string.enter_passphrase);
            getActivity().getActionBar().setTitle(R.string.enter_passphrase);
        } else if(selectedAction.equals(MainActivity.CREATE_METHOD)){
            passphraseAgain.setVisibility(View.VISIBLE);
            passphraseTextAgain.setVisibility(View.VISIBLE);
            passphraseText.setText(R.string.passphrase);
            getActivity().getActionBar().setTitle(R.string.set_passphrase);
        }
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Uri uri);
    }
}
