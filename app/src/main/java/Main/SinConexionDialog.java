package Main;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;


public class SinConexionDialog extends AppCompatDialogFragment{
    private EditText editNombre;
    private EditText editDescripcion;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.sin_conexion_dialog_layout, null);

        editNombre = view.findViewById(R.id.edit_nombre);
        editDescripcion = view.findViewById(R.id.edit_descripcion);

        builder.setView(view)
                .setTitle("Modo Sin Conexión")
                .setNegativeButton("cancelar", (dialogInterface, i) -> {
                    dialogInterface.cancel();
                })
                .setCancelable(false)
                .setPositiveButton("ok", (dialogInterface, i) -> {
                    String nombre = editNombre.getText().toString();
                    String descripcion = editDescripcion.getText().toString();

                    if(nombre.equals("")){
                        Toast.makeText(MainActivity.getInstance(), "Por favor pon un nombre", Toast.LENGTH_SHORT).show();
                        MainActivity.getInstance().noConnectionDialog();
                    }
                    else{
                        MainActivity.getInstance().guardarEnDB(nombre,descripcion);
                    }
                });
        return builder.create();
    }
}