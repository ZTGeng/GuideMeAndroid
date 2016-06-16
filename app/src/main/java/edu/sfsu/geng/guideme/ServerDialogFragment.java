package edu.sfsu.geng.guideme;


import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;


/**
 * A simple {@link Fragment} subclass.
 */
public class ServerDialogFragment extends DialogFragment {


    private ServerDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.server_dialog_layout, null);
        builder.setMessage(R.string.change_server_hint)
                .setView(dialogView)
                .setPositiveButton(R.string.change_server_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String serverUrl = ((EditText) dialogView.findViewById(R.id.server_url_text)).getText().toString();
                        mListener.onServerChanged(serverUrl);
                    }
                })
                .setNeutralButton(R.string.change_server_default, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onUseDefaultServer();
                    }
                })
                .setNegativeButton(R.string.change_server_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        getDialog().cancel();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ServerDialogListener) activity;
        } catch (ClassCastException e) {
            throw new RuntimeException(activity.toString()
                    + " must implement ServerDialogListener");
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
    public interface ServerDialogListener {
        void onServerChanged(String serverUrl);
        void onUseDefaultServer();
    }

}
