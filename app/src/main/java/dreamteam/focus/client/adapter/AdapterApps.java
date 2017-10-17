package dreamteam.focus.client.adapter;

import android.content.Context;
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

import java.util.ArrayList;

import dreamteam.focus.R;
import dreamteam.focus.client.CreateProfile;


public class AdapterApps extends ArrayAdapter<String> {


    public AdapterApps(Context context,ArrayList<String> appArray)
    {
        super(context,0, appArray);
    }

    @Nullable
    @NonNull
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent)
    {
        final String appName = getItem(position);
        int index = CreateProfile.appsOnDevice.indexOf(appName);
        final String packageName=CreateProfile.packagesOnDevice.get(index);
        final TextView textAppName;
        CheckBox appStatus;

         View   conView = LayoutInflater.from(getContext()).inflate(R.layout.app_listitem , parent, false);


         textAppName=(TextView)conView.findViewById(R.id.textViewAppName);
        textAppName.setText(appName);

         appStatus=(CheckBox) conView.findViewById(R.id.checkBoxAppStatus);

        if (CreateProfile.blockedPackages.contains(packageName))
        {
            Log.e("BlockedPackage",packageName+" "+appName+" "+position);
            appStatus.setChecked(true);
        }


        appStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(CreateProfile.blockedPackages.contains(appName))
                {
                    if(!isChecked){
                        CreateProfile.blockedPackages.remove(packageName);

                    }

                }
                else
                {
                    if(isChecked)
                    {
                        CreateProfile.blockedPackages.add(packageName);
                    }
                }
            }
        });

        return conView;
    }
}