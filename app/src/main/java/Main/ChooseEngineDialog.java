package Main;

import static android.content.Context.LOCATION_SERVICE;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class ChooseEngineDialog extends AppCompatDialogFragment implements View.OnClickListener {

    private Button btnMapbox, btnGoogleMaps;
    private UbicacionItem ubicacionItem;
    private ListActivity listActivity;

    public ChooseEngineDialog(ListActivity listActivity, UbicacionItem ubicacionItem) {
        this.listActivity = listActivity;
        this.ubicacionItem = ubicacionItem;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.choose_engine_dialog, null);

        btnMapbox = view.findViewById(R.id.btnMapbox);
        btnMapbox.setOnClickListener(this);

        btnGoogleMaps = view.findViewById(R.id.btnGoogleMaps);
        btnGoogleMaps.setOnClickListener(this);

        builder.setView(view)
                .setTitle("Elige que quieres utilizar:");
        return builder.create();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnMapbox:
                Intent intent = new Intent(listActivity, NavigationActivity.class);
                double[] arrayLatLng = new double[2];
                arrayLatLng[0] = ubicacionItem.getLat();
                arrayLatLng[1] = ubicacionItem.getLon();
                intent.putExtra("arrayLatLng", arrayLatLng);
                startActivity(intent);
                dismiss();
                break;
            case R.id.btnGoogleMaps:
                //Para que cada vez que clickes en un item te mande a la ubicación de google maps
                Uri mapUri = Uri.parse("geo:0,0?q=" + ubicacionItem.getLat() + "," + ubicacionItem.getLon() + "(Ubi: " + ubicacionItem.getNombre() + ")");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                listActivity.startActivity(mapIntent);
                dismiss();
                break;
        }
    }
}
