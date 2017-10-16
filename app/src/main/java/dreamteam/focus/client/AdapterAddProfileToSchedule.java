package dreamteam.focus.client;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;

import dreamteam.focus.Profile;
import dreamteam.focus.ProfileInSchedule;
import dreamteam.focus.R;
import dreamteam.focus.Schedule;
import dreamteam.focus.server.DatabaseConnector;

/**
 * Created by aarav on 10/14/17.
 */

public class AdapterAddProfileToSchedule extends ArrayAdapter<Profile> {

    private CheckBox appStatus;
    public Context context;
    private boolean check=false;
    private DatabaseConnector db;

    public AdapterAddProfileToSchedule(Context context, ArrayList<Profile> profilesArray)
    {
        super(context,0, profilesArray);
        this.context=context;
        db = new DatabaseConnector(getContext());
    }

    @Nullable
    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
       final Profile s = getItem(position);


        View conView = LayoutInflater.from(getContext()).inflate(R.layout.schedule_addprofile_item , parent, false);

        final TextView textAppName=(TextView)conView.findViewById(R.id.textViewProfileName);
        appStatus =(CheckBox) conView.findViewById(R.id.checkBoxProfile2);


        textAppName.setText(s.getName());

        appStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    Log.e("error","iIS CHECKED!!!!!");
                    AddProfileToSchedule.profilesToBeBlocked.add(s);
                }
                else
                {
                    if(AddProfileToSchedule.profilesToBeBlocked.contains(s)){
                        AddProfileToSchedule.profilesToBeBlocked.remove(s);
                    }
                }
            }
        });

        return conView;
    }
}